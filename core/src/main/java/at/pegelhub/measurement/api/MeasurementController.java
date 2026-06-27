package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.api.read.MeasurementReadResponseMapper;
import at.pegelhub.measurement.api.read.input.MeasurementBucketParameters;
import at.pegelhub.measurement.api.read.input.MeasurementPageParameters;
import at.pegelhub.measurement.api.read.output.MeasurementBucketListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementListResponse;
import at.pegelhub.measurement.api.write.MeasurementWriteRequestMapper;
import at.pegelhub.measurement.api.write.WriteMeasurementsRequest;
import at.pegelhub.measurement.application.MeasurementService;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
public class MeasurementController implements MeasurementApi {

    private final MeasurementService measurementService;
    private final MeasurementReadQueryResolver queryResolver;

    public MeasurementController(MeasurementService measurementService, MeasurementReadQueryResolver queryResolver) {
        this.measurementService = requireNonNull(measurementService);
        this.queryResolver = requireNonNull(queryResolver);
    }

    @Override
    public void writeMeasurementData(WriteMeasurementsRequest measurements) {
        measurementService.writeMeasurements(MeasurementWriteRequestMapper.convert(measurements));
    }

    @Override
    public MeasurementListResponse listMeasurements(
            UUID timeSeriesId,
            MeasurementPageParameters parameters) {
        return MeasurementReadResponseMapper.toResponse(measurementService.listMeasurements(
                queryResolver.resolvePage(timeSeriesId, parameters)));
    }

    @Override
    public MeasurementBucketListResponse listMeasurementBuckets(
            UUID timeSeriesId,
            MeasurementBucketParameters parameters) {
        return MeasurementReadResponseMapper.toResponse(measurementService.listMeasurementBuckets(
                queryResolver.resolveBuckets(timeSeriesId, parameters)));
    }

    @Override
    public Instant getSystemTime() {
        return measurementService.getSystemTime();
    }
}
