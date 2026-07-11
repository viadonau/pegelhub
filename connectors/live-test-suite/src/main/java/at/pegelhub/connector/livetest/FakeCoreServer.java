package at.pegelhub.connector.livetest;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class FakeCoreServer implements AutoCloseable {
    private static final Gson GSON = new Gson();

    private final FakeCoreState state;
    private final HttpServer server;

    FakeCoreServer(int port, FakeCoreState state) throws IOException {
        this.state = state;
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.server.createContext("/", this::handle);
    }

    void start() {
        server.start();
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        state.request(new CoreRequest(state.name(), exchange.getRequestMethod(), path, query, auth, Instant.now()));

        if (!("Bearer " + SuiteConstants.TOKEN).equals(auth)) {
            HttpSupport.respond(exchange, 401, "{\"error\":\"missing_or_invalid_token\"}");
            return;
        }

        if ("POST".equals(exchange.getRequestMethod()) && "/api/v1/measurements".equals(path)) {
            handleMeasurementWrite(exchange);
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())
                && path.startsWith("/api/v1/time-series/")
                && path.endsWith("/measurements")) {
            handleMeasurementRead(exchange, path, query);
            return;
        }

        HttpSupport.respond(exchange, 404, "{\"error\":\"not_found\"}");
    }

    private void handleMeasurementWrite(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        IncomingBatch batch = GSON.fromJson(HttpSupport.readBody(exchange), IncomingBatch.class);
        if (batch == null || batch.measurements == null) {
            HttpSupport.respond(exchange, 400, "{\"error\":\"missing_measurements\"}");
            return;
        }
        for (IncomingMeasurement measurement : batch.measurements) {
            state.write(new MeasurementRecord(
                    UUID.fromString(measurement.timeSeriesId),
                    Instant.parse(measurement.observedAt),
                    measurement.value));
        }
        HttpSupport.respondNoContent(exchange);
    }

    private void handleMeasurementRead(com.sun.net.httpserver.HttpExchange exchange, String path, String query) throws IOException {
        String prefix = "/api/v1/time-series/";
        String id = path.substring(prefix.length(), path.length() - "/measurements".length());
        UUID timeSeriesId = UUID.fromString(id);
        var params = HttpSupport.query(query);
        List<MeasurementRecord> measurements = state.seeded(timeSeriesId);
        if ("desc".equalsIgnoreCase(params.get("order"))) {
            measurements = measurements.stream()
                    .sorted(Comparator.comparing(MeasurementRecord::observedAt).reversed())
                    .toList();
        } else {
            measurements = measurements.stream()
                    .sorted(Comparator.comparing(MeasurementRecord::observedAt))
                    .toList();
        }
        int limit = parseLimit(params.get("limit"), measurements.size());
        if (limit < measurements.size()) {
            measurements = measurements.subList(0, limit);
        }
        HttpSupport.respond(exchange, 200, measurementListJson(timeSeriesId, measurements));
    }

    private static int parseLimit(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Math.max(0, Integer.parseInt(raw));
    }

    private static String measurementListJson(UUID timeSeriesId, List<MeasurementRecord> measurements) {
        StringBuilder json = new StringBuilder();
        json.append("{\"timeSeriesId\":\"").append(timeSeriesId).append("\",");
        json.append("\"window\":null,\"order\":\"ASC\",\"limit\":1000,\"truncated\":false,\"next\":null,");
        json.append("\"measurements\":[");
        for (int i = 0; i < measurements.size(); i++) {
            MeasurementRecord measurement = measurements.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"observedAt\":\"").append(measurement.observedAt()).append("\",");
            json.append("\"value\":").append(measurement.value()).append('}');
        }
        json.append("]}");
        return json.toString();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static final class IncomingBatch {
        List<IncomingMeasurement> measurements;
    }

    private static final class IncomingMeasurement {
        String timeSeriesId;
        String observedAt;
        Double value;
    }
}
