package at.pegelhub.lib.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

final class ConnectorResourceCloser {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorResourceCloser.class);

    private ConnectorResourceCloser() {
    }

    static void closeInReverse(String owner, List<? extends AutoCloseable> resources) {
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception e) {
                LOG.error("Failed closing a resource owned by {}.", owner, e);
            }
        }
    }
}
