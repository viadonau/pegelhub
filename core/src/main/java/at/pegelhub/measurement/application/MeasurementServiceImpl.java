package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.supplier.domain.Supplier;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.auth.application.AuthTokenIdHolder;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.supplier.persistence.SupplierRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code MeasurementService}.
 */
@Service
public final class MeasurementServiceImpl implements MeasurementService {

    private final SupplierRepository supplierRepository;
    private final MeasurementRepository measurementRepository;

    public MeasurementServiceImpl(SupplierRepository supplierRepository, MeasurementRepository measurementRepository) {
        this.supplierRepository = requireNonNull(supplierRepository);
        this.measurementRepository = requireNonNull(measurementRepository);
    }

    /**
     * processes the measurements to be saved to the time series database
     * @param writeMeasurements to save.
     */
    @Override
    public void writeMeasurements(WriteMeasurements writeMeasurements) {
        UUID supplierId = supplierRepository.getSupplierIdForAuthId(AuthTokenIdHolder.get());
        if (supplierId == null) {
            throw new NotFoundException("Supplier not yet registered");
        }
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
            measurements.add(new Measurement(supplierId, measurement.timestamp(), measurement.fields(), measurement.infos()));
        }
        measurementRepository.storeMeasurements(measurements);
    }

    /**
     * @param range in which the returned values reside.
     * @return all saved measurements in the specified range
     */
    @Override
    public List<Measurement> getByRange(String range) {
        return measurementRepository.getByRange(range);
    }

    /**
     * @param stationNumber of the supplier
     * @param range in which the returned values reside.
     * @return the measurements from a specific station in a specified range
     */
    @Override
    public List<Measurement> getBySupplierAndRange(String stationNumber, String range) {
        Optional<Supplier> supplier = supplierRepository.findByStationNumber(stationNumber);
        if(supplier.isPresent()){
            return measurementRepository.getByIDAndRange(supplier.get().getId(),range);
        }
        throw new NotFoundException("No data found for given supplier");
    }

    @Override
    public Measurement getLatestBySupplier(String stationNumber) {
        Optional<Supplier> supplier = supplierRepository.findByStationNumber(stationNumber);
        if(supplier.isPresent()){
            return measurementRepository.getLastData(supplier.get().getId());
        }
        throw new NotFoundException("No data found for given supplier");
    }

    @Override
    public Measurement getAverageBySupplierAndRange(String stationNumber, String range) {
        Optional<Supplier> supplier = supplierRepository.findByStationNumber(stationNumber);
        if(supplier.isPresent()){
            return measurementRepository.getAverageByIdAndRange(supplier.get().getId(), range);
        }
        throw new NotFoundException("No data found for given supplier");
    }

    /**
     *
      * @param uuid of the measurement.
     * @return gets the last {@link Measurement} with the specified {@link UUID}
     */
    @Override
    public Measurement getLastData(UUID uuid) {
        return measurementRepository.getLastData(uuid);
    }

    public Timestamp getSystemTime()
    {
        return measurementRepository.getSystemTime();
    }
}
