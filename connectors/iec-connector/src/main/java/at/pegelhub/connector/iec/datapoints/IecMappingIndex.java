package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.MappingDirection;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * Loads mappings once and rejects duplicate IOAs, including duplicates across directions.
 * Changing the supplied list later does not affect this index. Outgoing mappings are returned in configuration order.
 */
@Slf4j
public final class IecMappingIndex {

    private final Map<Integer, DataPointMapping> mappingsByIoa;

    public IecMappingIndex(List<DataPointMapping> mappings) {
        Objects.requireNonNull(mappings, "mappings");
        Map<Integer, DataPointMapping> byIoa = new LinkedHashMap<>();
        for (DataPointMapping mapping : mappings) {
            if (byIoa.putIfAbsent(mapping.iecIoa(), mapping) != null) {
                throw new IllegalArgumentException("Duplicate IOA " + mapping.iecIoa());
            }

            log.debug(
                    "Loaded datapoint: IOA={}, timeSeriesId={}, direction={}",
                    mapping.iecIoa(),
                    mapping.timeSeriesId(),
                    mapping.direction());
        }
        mappingsByIoa = Collections.unmodifiableMap(byIoa);

        log.info(
                "Loaded datapoints -> protocolToCore={}, coreToProtocol={}",
                protocolToCoreIoas().size(),
                coreToProtocolMappings().size());
    }

    /**
     * Looks up an IOA without checking its direction. Use {@link #protocolToCoreIoas()} when filtering incoming data.
     */
    public Optional<UUID> getTimeSeriesId(int ioa) {
        return Optional.ofNullable(mappingsByIoa.get(ioa)).map(DataPointMapping::timeSeriesId);
    }

    public Set<Integer> protocolToCoreIoas() {
        return mappingsByIoa.values().stream()
                .filter(mapping -> mapping.direction() == MappingDirection.EXTERNAL_TO_CORE)
                .map(DataPointMapping::iecIoa)
                .collect(toUnmodifiableSet());
    }

    public List<DataPointMapping> coreToProtocolMappings() {
        return mappingsByIoa.values().stream()
                .filter(mapping -> mapping.direction() == MappingDirection.CORE_TO_EXTERNAL)
                .toList();
    }
}
