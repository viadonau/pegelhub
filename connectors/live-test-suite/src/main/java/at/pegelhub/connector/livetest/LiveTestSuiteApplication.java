package at.pegelhub.connector.livetest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

public final class LiveTestSuiteApplication {
    private LiveTestSuiteApplication() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "server" : args[0];
        switch (mode) {
            case "server" -> runServer();
            case "verify" -> runVerifier(args.length > 1 ? args[1] : getenv("LIVE_SCENARIO", "all"));
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    private static void runServer() throws Exception {
        LiveHarness harness = new LiveHarness();
        harness.start();
        Runtime.getRuntime().addShutdownHook(new Thread(harness::close, "live-test-suite-shutdown"));
        System.out.println("PegelHub live connector suite harness started on admin port " + SuiteConstants.ADMIN_PORT);
        new CountDownLatch(1).await();
    }

    private static void runVerifier(String scenario) throws Exception {
        String adminUrl = getenv("LIVE_SUITE_ADMIN_URL", "http://live-suite:" + SuiteConstants.ADMIN_PORT);
        int timeoutSeconds = Integer.parseInt(getenv("LIVE_VERIFY_TIMEOUT_SECONDS", "90"));
        URI verifyUri = URI.create(adminUrl + "/verify?scenario=" + scenario);
        HttpClient client = HttpClient.newHttpClient();
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        String lastBody = "";
        int lastStatus = 0;

        while (Instant.now().isBefore(deadline)) {
            try {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(verifyUri).timeout(Duration.ofSeconds(5)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                lastBody = response.body();
                if (response.statusCode() == 200) {
                    System.out.print(response.body());
                    return;
                }
            } catch (Exception e) {
                lastBody = e.toString();
            }
            Thread.sleep(1000);
        }

        System.err.println("Live connector suite did not pass before timeout.");
        System.err.println("Last verifier status: " + lastStatus);
        System.err.println(lastBody);
        System.exit(1);
    }

    private static String getenv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
