package at.pegelhub.connector.icc;

import at.pegelhub.lib.CoreConnection;

import java.time.Duration;
import java.util.List;

record IccConnectorSettings(
        CoreConnection coreConnection,
        CoreConnection externalConnection,
        Duration pollInterval,
        List<IccMapping> mappings) {
}
