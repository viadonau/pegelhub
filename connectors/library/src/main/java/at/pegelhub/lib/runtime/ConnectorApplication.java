package at.pegelhub.lib.runtime;

import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

public final class ConnectorApplication {
    private static final Path DEFAULT_CONFIG_DIRECTORY = Path.of("/app/config");
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorApplication.class);

    private ConnectorApplication() {
    }

    public static ConnectorRuntime start(String[] args, ConnectorModule module) throws Exception {
        Objects.requireNonNull(args, "args");
        Path configDirectory = args.length == 0 ? DEFAULT_CONFIG_DIRECTORY : Path.of(args[0]);
        return start(module, ConnectorConfigDirectory.at(configDirectory), PegelHubClientFactory.http());
    }

    public static ConnectorRuntime start(
            ConnectorModule module,
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(configDirectory, "configDirectory");
        Objects.requireNonNull(coreClients, "coreClients");
        ConnectorRuntimeDefinition definition = module.define(configDirectory, coreClients);
        ConnectorRuntime runtime = ConnectorRuntime.start(definition);
        LOG.info("Started {}", definition.name());
        return runtime;
    }

    public static void run(String[] args, ConnectorModule module) {
        try {
            ConnectorRuntime runtime = start(args, module);
            runtime.addShutdownHook();
        } catch (Exception e) {
            LOG.error("Failed to start {}", module.name(), e);
            System.exit(1);
        }
    }
}
