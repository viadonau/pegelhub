package at.pegelhub.testsupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Configuration of the InfluxDB database.
 */
public class PegelHubPostgresqlContainer extends PostgreSQLContainer<PegelHubPostgresqlContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PegelHubPostgresqlContainer.class);

    private static final String IMAGE_VERSION = "postgres:14-alpine";
    private static PegelHubPostgresqlContainer containerInstance;

    // ensure that the container instance won't be overridden by concurrent test executions
    private static final ReentrantLock MUTEX = new ReentrantLock();

    private PegelHubPostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    public static PegelHubPostgresqlContainer getInstance() {
        MUTEX.lock();
        try {
            if (containerInstance == null) {
                containerInstance = new PegelHubPostgresqlContainer()
                        .withDatabaseName("integration-test-db")
                        .withUsername("test")
                        .withPassword("test")
                        .withReuse(false)
                        .withLabel("reuse.UUID", "c7831930-4a99-4f18-a5dc-c5c80085bc46");
            }
            return containerInstance;
        } finally {
            MUTEX.unlock();
        }
    }

    @Override
    public void start() {
        if (containerInstance.isRunning()) {
            LOGGER.info("PostgreSQL testcontainer is already running and will be reused!");
        } else {
            Instant start = Instant.now();
            super.start();
            LOGGER.info("PostgreSQL testcontainer with image {} started in {}",
                    PegelHubPostgresqlContainer.IMAGE_VERSION,
                    Duration.between(start, Instant.now()));
        }
    }
}
