package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.MeasurementRepresentation;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
public final class IecMappingIndex {

    private final Set<Integer> protocolToCore = new HashSet<>();
    private final Set<Integer> coreToProtocol = new HashSet<>();
    private final Map<Integer, UUID> timeSeriesIds = new HashMap<>();
    private final Map<Integer, MeasurementRepresentation> outputRepresentations = new HashMap<>();

    public IecMappingIndex(List<DataPointMapping> mappings) {
        Objects.requireNonNull(mappings, "mappings");
        loadDataPoints(mappings);
        log.info(
                "Loaded datapoints -> protocolToCore={}, coreToProtocol={}",
                protocolToCore.size(),
                coreToProtocol.size());
    }

    public Optional<UUID> getTimeSeriesId(int ioa) {
        return Optional.ofNullable(timeSeriesIds.get(ioa));
    }

    public MeasurementRepresentation getOutputRepresentation(int ioa) {
        return Objects.requireNonNull(outputRepresentations.get(ioa), "Unknown IOA " + ioa);
    }

    public Set<Integer> protocolToCoreIoas() {
        return Collections.unmodifiableSet(protocolToCore);
    }

    public Set<Integer> coreToProtocolIoas() {
        return Collections.unmodifiableSet(coreToProtocol);
    }

    private void loadDataPoints(List<DataPointMapping> mappings) {
        Set<Integer> seen = new HashSet<>();
        for (DataPointMapping mapping : mappings) {
            int ioa = mapping.iecIoa();
            if (!seen.add(ioa)) {
                throw new IllegalArgumentException("Duplicate IOA " + ioa);
            }

            if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
                protocolToCore.add(ioa);
            } else {
                coreToProtocol.add(ioa);
            }

            timeSeriesIds.put(ioa, mapping.timeSeriesId());
            outputRepresentations.put(ioa, mapping.outputRepresentation());

            log.debug(
                    "Loaded datapoint: IOA={}, timeSeriesId={}, direction={}",
                    ioa,
                    mapping.timeSeriesId(),
                    mapping.direction());
        }
    }

}
