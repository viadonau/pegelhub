package at.pegelhub.connector.ma;

import lombok.extern.slf4j.Slf4j;
import at.pegelhub.connector.ma.config.MaConfigLoader;
import at.pegelhub.connector.ma.config.MaConnectorOptions;
import at.pegelhub.connector.ma.core.MaReadJob;
import at.pegelhub.connector.ma.core.InputRegistry;
import at.pegelhub.connector.ma.core.MaConnectorScheduler;
import at.pegelhub.connector.ma.jni.RevPiReader;
import at.pegelhub.connector.ma.jni.RevPiReaderImpl;

import java.net.URI;
import java.net.URL;

@Slf4j
public class MaConnectorApplication {

    /**
     * Boots the connector, loads configuration, initializes JNI, and starts scheduling.
     *
     * @param args optional config directory at index 0
     */
    public static void main(String[] args) {
        try {
            MaConfigLoader configLoader = new MaConfigLoader();
            MaConnectorOptions config = configLoader.parseConfig(args);
            URL coreBaseUrl = URI.create("http://" + config.coreAddress() + ":" + config.corePort() + "/").toURL();

            RevPiReader revPiReader = new RevPiReaderImpl();
            InputRegistry inputRegistry = new InputRegistry(revPiReader, config.inputsDir(), coreBaseUrl);
            MaReadJob readJob = new MaReadJob(inputRegistry, revPiReader);
            MaConnectorScheduler maConnectorService = new MaConnectorScheduler(readJob, config.delay());

            inputRegistry.loadInputs();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    log.info("Shutting down mA Connector");
                    maConnectorService.stop();
                    revPiReader.close();
                } catch (Exception ignored) {
                }
            }));

            maConnectorService.start();
            log.info("Started MaConnector");
        } catch (Exception e) {
            log.info("Failed to start mA Connector: {}", e.getMessage());
            System.exit(1);
        }
    }
}
