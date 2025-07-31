package com.stm.pegelhub.connector.iec.app;

import com.stm.pegelhub.connector.iec.datapoints.DataPointRegistry;
import com.stm.pegelhub.connector.iec.iec.IecClient;
import com.stm.pegelhub.connector.iec.iec.impl.IecClientImpl;
import com.stm.pegelhub.connector.iec.config.ConnectorOptions;
import com.stm.pegelhub.connector.iec.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

@Slf4j
public class IecConnectorApplication {

    public static void main(String[] args)  {
        log.info("Starting IEC Connector");
        try {
            ConfigLoader configService = new ConfigLoader();
            ConnectorOptions config = configService.parseConfig(args);

            IecConnectorService connectorService = getIecConnectorService(config);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down IEC Connector");
                connectorService.stop();
            }));

            connectorService.start();
        } catch (Exception e) {
            log.info("Failed to start IEC Connector: {}", e.getMessage());
            System.exit(1);
        }
    }

    private static IecConnectorService getIecConnectorService(ConnectorOptions config) throws Exception {
        URL coreBaseUrl = new URL("http", config.coreAddress(), config.corePort(), "/");
        DataPointRegistry dataPointRegistry = new DataPointRegistry(config.dataPointsDir(), coreBaseUrl);

        IecClient communicator = new IecClientImpl(
                config.iec_host(),
                config.iec_port(),
                config.common_address(),
                dataPointRegistry.supplierIoas()
        );

        return new IecConnectorService(config.delay(), communicator, dataPointRegistry);
    }
}


//TODO look into telemetry
//    telTask = new TimerTask()
//    {
//        public void run()
//        {
//            try {
//                Telemetry test = new Telemetry();
//                test.setCycleTime(cycleTime.toMillis());
//                test.setFieldStrengthTransmission(20.0);
//                test.setPerformanceElectricityBattery(20.0);
//                test.setPerformanceVoltageBattery(20.0);
//                test.setPerformanceElectricitySupply(null);
//                test.setPerformanceVoltageSupply(20.0);
//                test.setStationIPAddressExtern(Inet4Address.getLocalHost().getHostAddress());
//                test.setStationIPAddressIntern(InetAddress.getLocalHost().toString());
//                test.setTemperatureAir(20.0);
//                test.setTemperatureWater(20.0);
//                test.setMeasurement("Measurement Placeholder");
//                communicator.sendTelemetry(test);
//            } catch (UnknownHostException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    };
//    telInterval.scheduleAtFixedRate(telTask, 0, cycleTime.toMillis());


//TODO look into channel usage, why was this implemented
// For testing purposes: Each Connector only serves one Supplier. Need to clarify later
//    var infos = new HashMap<String, String>();
//                        if (work != null) {
//        infos.put("ChannelUse", work.getChannelUse());
//        infos.put("Type", work.getChannelUse());
//    } else {
//        infos.put("ChannelUse", "Placeholder");
//        infos.put("Type", "Placeholder");
//    }
