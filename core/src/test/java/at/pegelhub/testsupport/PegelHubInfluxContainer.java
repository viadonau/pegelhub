package at.pegelhub.testsupport;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Configuration of the InfluxDB database, including its configuration and initialization.
 */
public class PegelHubInfluxContainer extends InfluxDBContainer<PegelHubInfluxContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PegelHubInfluxContainer.class);

    private static final String IMAGE_VERSION = "influxdb:2.2-alpine";
    public static final String ADMIN_TOKEN = "integration-test-influx-token-000000000000000000000000";
    public static final String ORG = "pegelhub";
    public static final String DATA_BUCKET = "data";
    public static final String TELEMETRY_BUCKET = "telemetry";
    private static PegelHubInfluxContainer containerInstance;

    // ensure that the container instance won't be overridden by concurrent test executions
    private static final ReentrantLock MUTEX = new ReentrantLock();

    private PegelHubInfluxContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));
    }

    public static PegelHubInfluxContainer getInstance() {
        MUTEX.lock();
        try {
            if (containerInstance == null) {
                @SuppressWarnings("resource")
                PegelHubInfluxContainer container = new PegelHubInfluxContainer()
                        .withUsername("test")
                        .withPassword("test1234")
                        .withAdmin("testAdmin")
                        .withAdminPassword("testAdmin")
                        .withAdminToken(ADMIN_TOKEN)
                        .withOrganization(ORG)
                        .withBucket(DATA_BUCKET)
                        .withReuse(false)
                        .withLabel("reuse.UUID", "d4531930-4a99-4f18-a5dc-c5c80085bc46");
                containerInstance = container;
            }
            return containerInstance;
        } finally {
            MUTEX.unlock();
        }
    }

    @Override
    public void start() {
        if (containerInstance.isRunning()) {
            LOGGER.info("InfluxDB testcontainer is already running and will be reused!");
        } else {
            Instant start = Instant.now();
            super.start();
            LOGGER.info("InfluxDB testcontainer with image {} started in {}",
                    PegelHubInfluxContainer.IMAGE_VERSION,
                    Duration.between(start, Instant.now()));
            initInflux();
        }
    }

    private void initInflux() {
        try (InfluxDBClient initClient = InfluxDBClientFactory.create(
                this.getUrl(), ADMIN_TOKEN.toCharArray(), ORG, DATA_BUCKET)) {
            Organization pegelhub = initClient.getOrganizationsApi().findOrganizations().stream()
                    .filter(organization -> ORG.equals(organization.getName()))
                    .findFirst()
                    .orElseGet(() -> initClient.getOrganizationsApi().createOrganization(ORG));

            ensureBucket(initClient, pegelhub, DATA_BUCKET);
            ensureBucket(initClient, pegelhub, TELEMETRY_BUCKET);
        }
    }

    private void ensureBucket(InfluxDBClient initClient, Organization organization, String bucketName) {
        Optional<Bucket> existingBucket = initClient.getBucketsApi().findBuckets().stream()
                .filter(bucket -> bucketName.equals(bucket.getName()))
                .findFirst();
        if (existingBucket.isEmpty()) {
            initClient.getBucketsApi().createBucket(bucketName, organization.getId());
        }
    }
}
