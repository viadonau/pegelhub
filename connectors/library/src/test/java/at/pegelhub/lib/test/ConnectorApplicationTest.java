package at.pegelhub.lib.test;

import at.pegelhub.lib.config.ScheduleConfig;
import at.pegelhub.lib.runtime.ConnectorApplication;
import at.pegelhub.lib.runtime.ConnectorContext;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorPlan;
import at.pegelhub.lib.runtime.ConnectorApplicationHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorApplicationTest {
    @TempDir
    Path configDir;

    @Test
    void startResolvesConfigDirAndRunsRealScheduledTask() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        AtomicReference<Path> seenConfigDir = new AtomicReference<>();

        ConnectorModule module = new ConnectorModule() {
            @Override
            public String name() {
                return "test connector";
            }

            @Override
            public ConnectorPlan plan(ConnectorContext context) {
                seenConfigDir.set(context.configDir());
                return ConnectorPlan.builder(name())
                        .fixedDelayTask("poll", ran::countDown, Duration.ofMillis(10))
                        .build();
            }
        };

        try (ConnectorApplicationHandle runtime = ConnectorApplication.start(new String[]{configDir.toString()}, module)) {
            assertTrue(ran.await(2, TimeUnit.SECONDS));
        }

        assertEquals(configDir, seenConfigDir.get());
    }

    @Test
    void startPropagatesModuleStartupFailure() {
        AtomicBoolean closed = new AtomicBoolean(false);
        ConnectorModule module = new ConnectorModule() {
            @Override
            public String name() {
                return "failing connector";
            }

            @Override
            public ConnectorPlan plan(ConnectorContext context) {
                return ConnectorPlan.builder(name())
                        .onStart(() -> {
                            throw new IllegalStateException("boom");
                        })
                        .closeOnStop(() -> closed.set(true))
                        .build();
            }
        };

        assertThrows(RuntimeException.class, () -> ConnectorApplication.start(new String[]{configDir.toString()}, module));
        assertTrue(closed.get());
    }

    @Test
    void loadYamlResolvesConfigDirAndParsesTypedYaml() throws Exception {
        Files.writeString(configDir.resolve("connector.yaml"), """
                delay: "42s"
                """);

        ConnectorContext context = ConnectorContext.fromArgs(new String[]{configDir.toString()});

        ScheduleConfig yaml = context.loadYaml("connector.yaml", ScheduleConfig.class);

        assertEquals("42s", yaml.delay());
    }
}
