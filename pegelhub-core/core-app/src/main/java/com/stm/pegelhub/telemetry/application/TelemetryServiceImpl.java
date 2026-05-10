package com.stm.pegelhub.telemetry.application;

import com.stm.pegelhub.telemetry.domain.Telemetry;
import com.stm.pegelhub.telemetry.application.TelemetryService;
import com.stm.pegelhub.telemetry.persistence.TelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code TelemetryService}.
 * Calls the repository-methods for storing and fetching the
 * telemtry data from the time series database
 * (Currently InfluxDB, name of bucket: telemetry=.
 */
@Service
public final class TelemetryServiceImpl implements TelemetryService {

    private final TelemetryRepository telemetryRepository;

    public TelemetryServiceImpl(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = requireNonNull(telemetryRepository);
    }

    /**
     * calls the {@link TelemetryRepository#saveTelemetry(Telemetry)} method.
     * @param telemetry telemetry-data to save.
     * @return the save telemetry-data.
     */
    @Override
    public Telemetry saveTelemetry(Telemetry telemetry) {
        return telemetryRepository.saveTelemetry(telemetry);
    }

    /**
     * calls the {@link TelemetryRepository#getByRange(String)}  method.
     * @param range The {@link String} from which all entries in the speciefed range shall be returned.
     * @return a {@link List<Telemetry>} from the specified time range.
     */
    @Override
    public List<Telemetry> getByRange(String range) {
        return telemetryRepository.getByRange(range);
    }

    /**
     * calls the {@link TelemetryRepository#getLastData(UUID)}  method.
     * @param uuid The {@link UUID} from which the last entry shall be returned.
     * @return the last entry from the given {@link UUID}.
     */
    @Override
    public Telemetry getLastData(UUID uuid) {
        return telemetryRepository.getLastData(uuid);
    }
}
