package at.pegelhub.lib.runtime;

import at.pegelhub.lib.PegelHubClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectorApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorApplication.class);

    private ConnectorApplication() {
    }

    public static ConnectorRuntime start(String[] args, ConnectorModule module) throws Exception {
        return start(args, module, PegelHubClientFactory.http());
    }

    public static ConnectorRuntime start(
            String[] args, ConnectorModule module, PegelHubClientFactory clientFactory) throws Exception {
        ConnectorBootstrap bootstrap = ConnectorBootstrap.fromArgs(args, clientFactory);
        return startRuntime(bootstrap, module);
    }

    private static ConnectorRuntime startRuntime(ConnectorBootstrap bootstrap, ConnectorModule module) throws Exception {
        ConnectorRuntimeDefinition definition = module.define(bootstrap);
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
