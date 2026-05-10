package com.stm.pegelhub.testsupport;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Organization;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Configuration of the InfluxDB database, including its configuration and initialization.
 */
public class PegelHubInfluxContainer extends InfluxDBContainer<PegelHubInfluxContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PegelHubInfluxContainer.class);

    private static final String IMAGE_VERSION = "influxdb:2.2-alpine";
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
                containerInstance = new PegelHubInfluxContainer()
                        .withUsername("test")
                        .withPassword("test1234")
                        .withAdmin("testAdmin")
                        .withAdminPassword("testAdmin")
                        .withBucket("test")
                        .withReuse(false)
                        .withLabel("reuse.UUID", "d4531930-4a99-4f18-a5dc-c5c80085bc46");
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
        try (InfluxDBClient initClient = InfluxDBClientFactory.create(this.getUrl(), "test", "test1234".toCharArray())) {
            Organization pegelhub = initClient.getOrganizationsApi().createOrganization("pegelhub");
            Bucket telemetry = initClient.getBucketsApi().createBucket("telemetry", pegelhub.getId());
            Bucket data = initClient.getBucketsApi().createBucket("data", pegelhub.getId());
            WriteApiBlocking writeApi = initClient.getWriteApiBlocking();
            writeApi.writePoints(
                    data.getName(), pegelhub.getName(),
                    List.of(
                            Point.measurement("e27efad2-b947-48b1-928e-c25663597f1c")
                                    .time(Instant.now().minus(36, ChronoUnit.HOURS), WritePrecision.MS)
                                    .addField("Wasserstand", 55.1)
                                    .addField("Abfluss", 10.1)
                                    .addField("WasserstandAbs", 67.1),
                            Point.measurement("e27efad2-b947-48b1-928e-c25663597f1c")
                                    .time(Instant.now().minus(12, ChronoUnit.HOURS), WritePrecision.MS)
                                    .addField("Wasserstand", 55.0)
                                    .addField("Abfluss", 10.0)
                                    .addField("WasserstandAbs", 67.0)
                    ));
            writeApi.writePoints(
                    telemetry.getName(), pegelhub.getName(),
                    List.of(
                            Point.measurement("1b89448c-effd-4bb1-bae9-bf6f13fd4d7c")
                                    .time(Instant.now().minus(12, ChronoUnit.HOURS), WritePrecision.MS)
                                    .addTag("stationIPAddressIntern", "255.255.255.0")
                                    .addTag("stationIPAddressExtern", "255.255.255.0")
                                    .addField("cycleTime", 10L)
                                    .addField("temperatureWater", 15.0)
                                    .addField("temperatureAir", 20.0)
                                    .addField("performanceVoltageBattery", 20.0)
                                    .addField("performanceVoltageSupply", 20.0)
                                    .addField("performanceElectricityBattery", 20.0)
                                    .addField("performanceElectricitySupply", 20.0)
                                    .addField("fieldStrengthTransmission", 20.0),
                            Point.measurement("e27efad2-b947-48b1-928e-c25663597f1c")
                                    .time(Instant.now().minus(36, ChronoUnit.HOURS), WritePrecision.MS)
                                    .addTag("stationIPAddressIntern", "255.255.255.0")
                                    .addTag("stationIPAddressExtern", "255.255.255.0")
                                    .addField("cycleTime", 11L)
                                    .addField("temperatureWater", 15.1)
                                    .addField("temperatureAir", 20.1)
                                    .addField("performanceVoltageBattery", 20.1)
                                    .addField("performanceVoltageSupply", 20.1)
                                    .addField("performanceElectricityBattery", 20.1)
                                    .addField("performanceElectricitySupply", 20.1)
                                    .addField("fieldStrengthTransmission", 20.1)
                    ));

        }
    }
}
