package at.pegelhub.connector.icc;

import at.pegelhub.lib.CoreConnection;

import java.time.Duration;
import java.util.List;

record IccConnectorOptions(
        CoreConnection coreConnection,
        CoreConnection externalConnection,
        Duration delay,
        String lookbackWindow,
        List<IccMapping> mappings) {
}
