package at.pegelhub.lib.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectorApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorApplication.class);

    private ConnectorApplication() {
    }

    public static ConnectorApplicationHandle start(String[] args, ConnectorModule module) throws Exception {
        return startRuntime(args, module);
    }

    private static ConnectorRuntime startRuntime(String[] args, ConnectorModule module) throws Exception {
        ConnectorContext context = ConnectorContext.fromArgs(args);
        ConnectorPlan plan = module.plan(context);
        ConnectorRuntime runtime = plan.toRuntime();
        runtime.start();
        LOG.info("Started {}", plan.name());
        return runtime;
    }

    public static void run(String[] args, ConnectorModule module) {
        try {
            ConnectorRuntime runtime = startRuntime(args, module);
            runtime.addShutdownHook();
        } catch (Exception e) {
            LOG.error("Failed to start {}", module.name(), e);
            System.exit(1);
        }
    }
}
