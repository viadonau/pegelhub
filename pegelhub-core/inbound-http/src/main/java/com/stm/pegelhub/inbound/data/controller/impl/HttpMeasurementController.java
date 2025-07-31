package com.stm.pegelhub.inbound.data.controller.impl;

import com.stm.pegelhub.common.model.data.Measurement;
import com.stm.pegelhub.inbound.data.controller.MeasurementController;
import com.stm.pegelhub.inbound.data.dto.WriteMeasurementsDto;
import com.stm.pegelhub.logic.service.data.MeasurementService;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.inbound.data.controller.impl.DtoToDomainConverter.convert;
import static java.util.Objects.requireNonNull;

/**
 * Controller implementation for {@code MeasurementController}.
 */
public class HttpMeasurementController implements MeasurementController {

    private final MeasurementService measurementService;

    public HttpMeasurementController(MeasurementService measurementService) {
        this.measurementService = requireNonNull(measurementService);
    }

    /**
     * @param apiKey needed because of interface, not used. The AuthorizedMeasurementController handles it
     * @param measurements the {@link WriteMeasurementsDto measurements} to be saved
     */
    @Override
    public void writeMeasurementData(String apiKey, WriteMeasurementsDto measurements) {
        measurementService.writeMeasurements(convert(measurements));
    }

    /**
     * @param apiKey needed because of interface, not used. The AuthorizedMeasurementController handles it
     * @param range the range in which the desired measurements reside
     * @return the {@link Measurement}s found in the specified range
     */
    @Override
    public List<Measurement> findMeasurementInRange(String apiKey, String range) {
        return measurementService.getByRange(range);
    }

    /**
     * @param apiKey needed because of interface, not used. The AuthorizedMeasurementController handles it
     * @param stationNumber the station Number/Supplier from which the measurements should originate
     * @param range the range in which the desired measurements reside
     * @return the measurements from the specified stationNumber/Supplier found in the specified range
     */
    @Override
    public List<Measurement> findMeasurementForSupplierInRange(String apiKey, String stationNumber, String range) {
        return measurementService.getBySupplierAndRange(stationNumber, range);
    }

    /**
     * @param apiKey needed because of interface, not used. The AuthorizedMeasurementController handles it
     * @param stationNumber the station Number/Supplier from which the latest measurement should originate
     * @return the latest measurements from the specified stationNumber/Supplier
     */
    @Override
    public Measurement findLatestMeasurementBySupplier(String apiKey, String stationNumber) {
        return measurementService.getLatestBySupplier(stationNumber);
    }

    /**
     * @param apiKey needed because of interface, not used. The AuthorizedMeasurementController handles it
     * @param uuid the {@link UUID} of the measurement to be searched for
     * @return the corresponding {@link Measurement} to the specified {@link UUID}
     */
    @Override
    public Measurement findMeasurementById(String apiKey, UUID uuid) {
        return measurementService.getLastData(uuid);
    }

    @Override
    public Timestamp getSystemtime() {
        return measurementService.getSystemTime();
    }
}
