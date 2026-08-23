package at.pegelhub.lib.runtime;

import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

public final class ConnectorApplication {
    private static final String VALIDATE_ARGUMENT = "--validate-config";
    private static final String VALIDATE_ENVIRONMENT = "PEGELHUB_VALIDATE_CONFIG";
    private static final Path DEFAULT_CONFIG_DIRECTORY = Path.of("/app/config");
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorApplication.class);

    private ConnectorApplication() {
    }

    public static ConnectorRuntime start(String[] args, ConnectorModule module) throws Exception {
        Objects.requireNonNull(args, "args");
        Path configDirectory = configDirectory(args, false);
        return start(module, ConnectorConfigDirectory.at(configDirectory), PegelHubClientFactory.http());
    }

    public static void validate(String[] args, ConnectorModule module) throws Exception {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(module, "module");
        ConnectorConfigDirectory configDirectory = ConnectorConfigDirectory.at(configDirectory(args, true));
        module.validate(configDirectory);
        LOG.info("Validated {} configuration in {}", module.name(), configDirectory.path());
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
            if (validationRequested(args)) {
                validate(args, module);
                return;
            }
            ConnectorRuntime runtime = start(args, module);
            runtime.addShutdownHook();
        } catch (Exception e) {
            LOG.error("Failed to start or validate {}", module.name(), e);
            System.exit(1);
        }
    }

    private static boolean validationRequested(String[] args) {
        return (args.length > 0 && VALIDATE_ARGUMENT.equals(args[0]))
                || Boolean.parseBoolean(System.getenv(VALIDATE_ENVIRONMENT));
    }

    private static Path configDirectory(String[] args, boolean validation) {
        int pathIndex = validation && args.length > 0 && VALIDATE_ARGUMENT.equals(args[0]) ? 1 : 0;
        if (args.length > pathIndex + 1) {
            throw new IllegalArgumentException("Expected at most one connector configuration directory.");
        }
        return args.length == pathIndex ? DEFAULT_CONFIG_DIRECTORY : Path.of(args[pathIndex]);
    }
}
