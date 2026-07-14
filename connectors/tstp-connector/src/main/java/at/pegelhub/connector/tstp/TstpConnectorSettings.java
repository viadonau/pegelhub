package at.pegelhub.connector.tstp;

import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.runtime.LoadedMapping;

import java.time.Duration;
import java.util.List;

record TstpConnectorSettings(
        CoreConnection coreConnection,
        String address,
        int port,
        Duration pollInterval,
        List<LoadedMapping<TstpMapping>> mappings) {
}
