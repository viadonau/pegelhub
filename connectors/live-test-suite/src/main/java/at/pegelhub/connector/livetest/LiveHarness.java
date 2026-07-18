package at.pegelhub.connector.livetest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class LiveHarness implements AutoCloseable {
    private final HarnessState state = new HarnessState();
    private final List<AutoCloseable> closeables = new ArrayList<>();

    void start() throws Exception {
        seedCoreData();
        start(new FakeKeycloakServer("local-keycloak", SuiteConstants.LOCAL_KEYCLOAK_PORT, state));
        start(new FakeKeycloakServer("external-keycloak", SuiteConstants.EXTERNAL_KEYCLOAK_PORT, state));
        start(new FakeCoreServer(SuiteConstants.LOCAL_CORE_PORT, state.localCore));
        start(new FakeCoreServer(SuiteConstants.EXTERNAL_CORE_PORT, state.externalCore));
        start(new FakeFtpService(fixturesDir()));
        start(new FakeTstpServer(state));
        start(new FakeIecServer(state));
        start(new AdminServer(state));
    }

    private void seedCoreData() {
        state.localCore.seed(state.tstpWriterFirst);
        state.localCore.seed(state.tstpWriterSecond);
        state.localCore.seed(state.iecCoreToExternalMeasurement);
        state.localCore.seed(state.iccLocalSourceMeasurement);
        state.externalCore.seed(state.iccExternalSourceMeasurement);
    }

    private Path fixturesDir() {
        String configured = System.getenv("LIVE_FIXTURES_DIR");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path repoRelative = Path.of("connectors/live-test-suite/fixtures");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }
        return Path.of("fixtures");
    }

    private void start(AutoCloseable service) throws Exception {
        if (service instanceof FakeKeycloakServer keycloak) {
            keycloak.start();
        } else if (service instanceof FakeCoreServer core) {
            core.start();
        } else if (service instanceof FakeFtpService ftp) {
            ftp.start();
        } else if (service instanceof FakeTstpServer tstp) {
            tstp.start();
        } else if (service instanceof FakeIecServer iec) {
            iec.start();
        } else if (service instanceof AdminServer admin) {
            admin.start();
        } else {
            throw new IllegalArgumentException("Unknown service " + service.getClass());
        }
        closeables.add(service);
    }

    @Override
    public void close() {
        for (int i = closeables.size() - 1; i >= 0; i--) {
            try {
                closeables.get(i).close();
            } catch (Exception ignored) {
            }
        }
    }
}
