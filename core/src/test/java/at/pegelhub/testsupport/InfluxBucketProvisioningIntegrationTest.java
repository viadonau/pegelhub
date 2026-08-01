package at.pegelhub.testsupport;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class InfluxBucketProvisioningIntegrationTest {

    private static final String ADMIN_TOKEN = "retention-test-token-000000000000000000000000000000";
    private static final String ORGANIZATION = "pegelhub";
    private static final String INTERNAL_BUCKET = "internal";
    private static final String DATA_BUCKET = "measurements";
    private static final String TELEMETRY_BUCKET = "telemetry";
    private static final long ONE_DAY_IN_SECONDS = 86_400L;
    private static final long THIRTY_DAYS_IN_SECONDS = 2_592_000L;
    private static final long SIXTY_DAYS_IN_SECONDS = 5_184_000L;
    private static final long EIGHT_WEEKS_IN_SECONDS = 4_838_400L;

    @Test
    void createsUpdatesAndReappliesBucketRetention() throws Exception {
        Path provisioner = Path.of(
                System.getProperty("basedir"),
                "docker/influxdb/configure-buckets.sh");

        try (TestInfluxContainer influx = new TestInfluxContainer()
                .withUsername("test")
                .withPassword("test1234")
                .withAdmin("admin")
                .withAdminPassword("admin1234")
                .withAdminToken(ADMIN_TOKEN)
                .withOrganization(ORGANIZATION)
                .withBucket(INTERNAL_BUCKET)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(provisioner),
                        "/usr/local/bin/configure-buckets.sh")) {
            influx.start();

            Container.ExecResult initialRun = configure(influx, "60d", "60d");
            assertThat(initialRun.getExitCode()).isZero();

            try (InfluxDBClient client = client(influx)) {
                Bucket internalBucket = bucket(client, INTERNAL_BUCKET);
                Bucket dataBucket = bucket(client, DATA_BUCKET);
                Bucket telemetryBucket = bucket(client, TELEMETRY_BUCKET);
                String internalBucketId = internalBucket.getId();
                String dataBucketId = dataBucket.getId();
                String telemetryBucketId = telemetryBucket.getId();

                assertThat(retentionSeconds(internalBucket)).isZero();
                assertThat(retentionSeconds(dataBucket)).isEqualTo(SIXTY_DAYS_IN_SECONDS);
                assertThat(retentionSeconds(telemetryBucket)).isEqualTo(SIXTY_DAYS_IN_SECONDS);

                Container.ExecResult alternateUnitsRun = configure(influx, "24h", "8w");
                assertThat(alternateUnitsRun.getExitCode()).isZero();
                assertThat(retentionSeconds(bucket(client, DATA_BUCKET))).isEqualTo(ONE_DAY_IN_SECONDS);
                assertThat(retentionSeconds(bucket(client, TELEMETRY_BUCKET))).isEqualTo(EIGHT_WEEKS_IN_SECONDS);

                Container.ExecResult secondRun = configure(influx, "0s", "30d");
                assertThat(secondRun.getExitCode()).isZero();

                Bucket updatedDataBucket = bucket(client, DATA_BUCKET);
                Bucket updatedTelemetryBucket = bucket(client, TELEMETRY_BUCKET);
                assertThat(updatedDataBucket.getId()).isEqualTo(dataBucketId);
                assertThat(updatedTelemetryBucket.getId()).isEqualTo(telemetryBucketId);
                assertThat(retentionSeconds(updatedDataBucket)).isZero();
                assertThat(retentionSeconds(updatedTelemetryBucket)).isEqualTo(THIRTY_DAYS_IN_SECONDS);

                Container.ExecResult idempotentRun = configure(influx, "0s", "30d");
                assertThat(idempotentRun.getExitCode()).isZero();
                assertThat(bucket(client, DATA_BUCKET).getId()).isEqualTo(dataBucketId);
                assertThat(bucket(client, TELEMETRY_BUCKET).getId()).isEqualTo(telemetryBucketId);
                assertThat(retentionSeconds(bucket(client, DATA_BUCKET))).isZero();
                assertThat(retentionSeconds(bucket(client, TELEMETRY_BUCKET)))
                        .isEqualTo(THIRTY_DAYS_IN_SECONDS);

                assertRejected(influx, "60d", "0d");
                assertRejected(influx, "-1d", "30d");
                assertRejected(influx, "30m", "30d");
                assertThat(configure(
                        influx,
                        INTERNAL_BUCKET,
                        "60d",
                        TELEMETRY_BUCKET,
                        "30d").getExitCode()).isNotZero();

                assertThat(retentionSeconds(bucket(client, DATA_BUCKET))).isZero();
                assertThat(retentionSeconds(bucket(client, TELEMETRY_BUCKET)))
                        .isEqualTo(THIRTY_DAYS_IN_SECONDS);
                assertThat(bucket(client, INTERNAL_BUCKET).getId()).isEqualTo(internalBucketId);
                assertThat(retentionSeconds(bucket(client, INTERNAL_BUCKET))).isZero();
            }
        }
    }

    private static Container.ExecResult configure(
            TestInfluxContainer influx,
            String dataRetention,
            String telemetryRetention) throws Exception {
        return configure(influx, DATA_BUCKET, dataRetention, TELEMETRY_BUCKET, telemetryRetention);
    }

    private static Container.ExecResult configure(
            TestInfluxContainer influx,
            String dataBucket,
            String dataRetention,
            String telemetryBucket,
            String telemetryRetention) throws Exception {
        return influx.execInContainer(
                "env",
                "INFLUX_HOST=http://localhost:8086",
                "INFLUX_ORG=" + ORGANIZATION,
                "INFLUX_TOKEN=" + ADMIN_TOKEN,
                "INFLUX_INTERNAL_BUCKET=" + INTERNAL_BUCKET,
                "INFLUX_DATA_BUCKET=" + dataBucket,
                "INFLUX_DATA_RETENTION=" + dataRetention,
                "INFLUX_TELEMETRY_BUCKET=" + telemetryBucket,
                "INFLUX_TELEMETRY_RETENTION=" + telemetryRetention,
                "sh",
                "/usr/local/bin/configure-buckets.sh");
    }

    private static void assertRejected(
            TestInfluxContainer influx,
            String dataRetention,
            String telemetryRetention) throws Exception {
        assertThat(configure(influx, dataRetention, telemetryRetention).getExitCode()).isNotZero();
    }

    private static InfluxDBClient client(TestInfluxContainer influx) {
        return InfluxDBClientFactory.create(
                influx.getUrl(),
                ADMIN_TOKEN.toCharArray(),
                ORGANIZATION,
                DATA_BUCKET);
    }

    private static Bucket bucket(InfluxDBClient client, String name) {
        return client.getBucketsApi().findBuckets().stream()
                .filter(bucket -> name.equals(bucket.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing InfluxDB bucket: " + name));
    }

    private static long retentionSeconds(Bucket bucket) {
        List<BucketRetentionRules> rules = bucket.getRetentionRules();
        if (rules == null || rules.isEmpty()) {
            return 0;
        }
        return rules.getFirst().getEverySeconds();
    }

    private static final class TestInfluxContainer extends InfluxDBContainer<TestInfluxContainer> {

        private TestInfluxContainer() {
            super(DockerImageName.parse("influxdb:2.2-alpine"));
        }
    }
}
