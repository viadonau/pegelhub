package at.pegelhub.lib.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ConnectorResources implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorResources.class);

    private final List<AutoCloseable> closeables = new ArrayList<>();
    private boolean transferred;

    private ConnectorResources() {
    }

    public static ConnectorResources create() {
        return new ConnectorResources();
    }

    public <T extends AutoCloseable> T add(T resource) {
        closeables.add(Objects.requireNonNull(resource, "resource"));
        return resource;
    }

    public void closeOnStop(AutoCloseable closeable) {
        closeables.add(Objects.requireNonNull(closeable, "closeable"));
    }

    public void release(AutoCloseable closeable) {
        closeables.remove(closeable);
    }

    public void transferTo(ConnectorPlan.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        for (AutoCloseable closeable : closeables) {
            builder.closeOnStop(closeable);
        }
        transferred = true;
    }

    @Override
    public void close() {
        if (transferred) {
            return;
        }
        List<AutoCloseable> reversed = new ArrayList<>(closeables);
        Collections.reverse(reversed);
        for (AutoCloseable closeable : reversed) {
            closeQuietly(closeable);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            LOG.warn("Failed closing connector resource after plan assembly failure", e);
        }
    }
}
