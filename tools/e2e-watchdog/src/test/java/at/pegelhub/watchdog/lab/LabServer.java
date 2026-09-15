package at.pegelhub.watchdog.lab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Test infrastructure only. The HTTP control surface is intentionally absent from the runtime image. */
public final class LabServer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] args) throws Exception {
        var iec = new IecFixture();
        var tstp = new TstpFixture();
        var traps = new TrapReceiver(1162);
        var secondTraps = new TrapReceiver(1163);
        var server = HttpServer.create(new InetSocketAddress(8030), 0);
        server.createContext("/", exchange -> handleRequest(exchange, iec, tstp, traps, secondTraps));
        server.start();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(iec::emit, 0, 1, TimeUnit.SECONDS);
    }

    private static void handleRequest(
            HttpExchange exchange,
            IecFixture iec,
            TstpFixture tstp,
            TrapReceiver traps,
            TrapReceiver secondTraps) throws IOException {
        try {
            var query = query(exchange);
            switch (exchange.getRequestURI().getPath()) {
                case "/control" -> configureFaults(exchange, query, iec, tstp);
                case "/status" -> respondJson(exchange, Map.of(
                        "iecPaused", iec.paused, "iecConnections", iec.connections(),
                        "tstp", tstp.status(), "traps", traps.messages(), "secondTraps", secondTraps.messages()));
                case "/traps" -> respondJson(exchange, Map.of(
                        "receiver1162", traps.packets(), "receiver1163", secondTraps.packets()));
                default -> respond(exchange, 200, tstp.handle(query, exchange.getRequestBody().readNBytes(2_000_000)));
            }
        } catch (Exception failure) {
            System.err.println("Fixture request failed: " + failure.getClass().getSimpleName());
            respond(exchange, 400, "Invalid fixture request");
        } finally {
            exchange.close();
        }
    }

    private static void configureFaults(
            HttpExchange exchange,
            Map<String, String> query,
            IecFixture iec,
            TstpFixture tstp) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            respond(exchange, 405, "POST required");
            return;
        }
        if (query.containsKey("iec")) {
            iec.paused = query.get("iec").equals("paused");
        }
        if (query.containsKey("tstp")) {
            tstp.configure(query.get("tstp"), Integer.parseInt(query.getOrDefault("delaySeconds", "60")));
        }
        respond(exchange, 200, "configured");
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String item : raw.split("&")) {
                String[] pair = item.split("=", 2);
                result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
            }
        }
        return result;
    }

    private static void respondJson(HttpExchange exchange, Object body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        respond(exchange, 200, JSON.toJson(body));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
