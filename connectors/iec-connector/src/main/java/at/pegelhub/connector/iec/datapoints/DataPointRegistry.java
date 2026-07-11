package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class DataPointRegistry implements AutoCloseable {

    private final Map<Integer, PegelHubClient> protocolToCore = new HashMap<>();
    private final Map<Integer, PegelHubClient> coreToProtocol = new HashMap<>();
    private final Map<Integer, UUID> timeSeriesIds = new HashMap<>();
    private final PegelHubClient client;

    public DataPointRegistry(List<DataPointMapping> mappings, PegelHubClient client) {
        Objects.requireNonNull(mappings, "mappings");
        this.client = Objects.requireNonNull(client, "client");
        loadDataPoints(mappings, client);
        log.info("Loaded datapoints -> protocolToCore={}, coreToProtocol={}",
                protocolToCore.size(), coreToProtocol.size());
    }

    public Optional<PegelHubClient> getProtocolToCoreClient(int ioa) {
        return Optional.ofNullable(protocolToCore.get(ioa));
    }

    public Optional<PegelHubClient> getCoreToProtocolClient(int ioa) {
        return Optional.ofNullable(coreToProtocol.get(ioa));
    }

    public Optional<UUID> getTimeSeriesId(int ioa) {
        return Optional.ofNullable(timeSeriesIds.get(ioa));
    }

    public Set<Integer> protocolToCoreIoas() {
        return Collections.unmodifiableSet(protocolToCore.keySet());
    }

    public Set<Integer> coreToProtocolIoas() {
        return Collections.unmodifiableSet(coreToProtocol.keySet());
    }

    private void loadDataPoints(List<DataPointMapping> mappings, PegelHubClient client) {
        Set<Integer> seen = new HashSet<>();
        for (DataPointMapping mapping : mappings) {
            int ioa = mapping.iecIoa();
            if (!seen.add(ioa)) {
                throw new IllegalArgumentException("Duplicate IOA " + ioa);
            }

            if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
                protocolToCore.put(ioa, client);
            } else {
                coreToProtocol.put(ioa, client);
            }
            timeSeriesIds.put(ioa, mapping.timeSeriesId());

            log.debug("Loaded datapoint: IOA={}, timeSeriesId={}, direction={}",
                    ioa, mapping.timeSeriesId(), mapping.direction());
        }
    }

    @Override
    public void close() {
        Set<PegelHubClient> clients = Collections.newSetFromMap(new IdentityHashMap<>());
        clients.add(client);
        clients.addAll(protocolToCore.values());
        clients.addAll(coreToProtocol.values());
        clients.forEach(this::closeClient);
    }

    private void closeClient(PegelHubClient client) {
        try {
            client.close();
        } catch (Exception e) {
            log.warn("Failed closing PegelHub client", e);
        }
    }

}
