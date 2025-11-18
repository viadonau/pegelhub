package com.stm.pegelhub.inbound.data.controller.authorization;

import com.stm.pegelhub.common.model.data.Measurement;
import com.stm.pegelhub.inbound.data.controller.MeasurementController;
import com.stm.pegelhub.inbound.data.dto.WriteMeasurementsDto;
import com.stm.pegelhub.logic.holder.AuthTokenIdHolder;
import com.stm.pegelhub.logic.service.metadata.AuthorizationService;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the Interface {@code MeasurementController}.
 * Performs authorization of requests where necessary before they are delegated to the underlying controller.
 */
public class AuthorizedMeasurementController implements MeasurementController {

    private final AuthorizationService authorizationService;
    private final MeasurementController delegate;

    public AuthorizedMeasurementController(AuthorizationService authorizationService, MeasurementController delegate) {
        this.authorizationService = requireNonNull(authorizationService);
        this.delegate = requireNonNull(delegate);
    }

    /**
     * authorizes the request and forwards it to for further handling
     * @param apiKey the key which is used for Authorization
     * @param measurement the measurement to be saved
     */
    @Override
    public synchronized void writeMeasurementData(String apiKey, WriteMeasurementsDto measurement) {

        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        delegate.writeMeasurementData(apiKey, measurement);
        AuthTokenIdHolder.clear();
    }

    /**
     * authorizes the request and forwards it to for further handling
     * @param apiKey the key which is used for Authorization
     * @param range the range in which the desired measurements reside
     * @return the measurements found in the specified range
     */
    @Override
    public List<Measurement> findMeasurementInRange(String apiKey, String range) {
        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        List<Measurement> measurements = delegate.findMeasurementInRange(apiKey, range);
        AuthTokenIdHolder.clear();
        return measurements;
    }

    /**
     * authorizes the request and forwards it to for further handling
     * @param apiKey the key which is used for Authorization
     * @param stationNumber the station Number/Supplier from which the measurements should originate
     * @param range the range in which the desired measurements reside
     * @return the measurements from the specified stationNumber/Supplier found in the specified range
     */
    @Override
    public List<Measurement> findMeasurementForSupplierInRange(String apiKey, String stationNumber, String range) {
        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        List<Measurement> measurements = delegate.findMeasurementForSupplierInRange(apiKey, stationNumber, range);
        AuthTokenIdHolder.clear();
        return measurements;
    }

    /**
     * authorizes the request and forwards it to for further handling
     * @param apiKey the key which is used for Authorization
     * @param stationNumber the stationNumber/Supplier of the measurement to be searched for
     * @return the latest measurement from the Supplier
     */
    @Override
    public Measurement findLatestMeasurementBySupplier(String apiKey, String stationNumber) {
        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        Measurement measurement = delegate.findLatestMeasurementBySupplier(apiKey, stationNumber);
        AuthTokenIdHolder.clear();
        return measurement;
    }

    @Override
    public Measurement findAverageMeasurementForSupplierInRange(String apiKey, String stationNumber, String range) {
        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        Measurement measurement = delegate.findAverageMeasurementForSupplierInRange(apiKey, stationNumber, range);
        AuthTokenIdHolder.clear();
        return measurement;
    }

    /**
     * authorizes the request and forwards it to for further handling
     * @param apiKey the key which is used for Authorization
     * @param uuid the {@link UUID} of the measurement to be searched for
     * @return the corresponding {@link Measurement} to the specified {@link UUID}
     */
    @Override
    public Measurement findMeasurementById(String apiKey, UUID uuid) {
        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        Measurement measurement = delegate.findMeasurementById(apiKey, uuid);
        AuthTokenIdHolder.clear();
        return measurement;
    }

    @Override
    public Timestamp getSystemtime() {
       return delegate.getSystemtime();
    }
}
