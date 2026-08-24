package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.MappingDirection;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
public final class IecMappingIndex {

    private final Set<Integer> protocolToCore = new HashSet<>();
    private final Set<Integer> coreToProtocol = new HashSet<>();
    private final Map<Integer, UUID> timeSeriesIds = new HashMap<>();
    private final Map<Integer, BigDecimal> gaugeZeroElevations = new HashMap<>();

    public IecMappingIndex(List<DataPointMapping> mappings) {
        Objects.requireNonNull(mappings, "mappings");
        loadDataPoints(mappings);
        log.info("Loaded datapoints -> protocolToCore={}, coreToProtocol={}",
                protocolToCore.size(), coreToProtocol.size());
    }

    public Optional<UUID> getTimeSeriesId(int ioa) {
        return Optional.ofNullable(timeSeriesIds.get(ioa));
    }

    public Optional<BigDecimal> getGaugeZeroElevationMAboveAdria(int ioa) {
        return Optional.ofNullable(gaugeZeroElevations.get(ioa));
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
            if (mapping.gaugeZeroElevationMAboveAdria() != null) {
                gaugeZeroElevations.put(ioa, mapping.gaugeZeroElevationMAboveAdria());
            }

            log.debug("Loaded datapoint: IOA={}, timeSeriesId={}, direction={}",
                    ioa, mapping.timeSeriesId(), mapping.direction());
        }
    }

}
