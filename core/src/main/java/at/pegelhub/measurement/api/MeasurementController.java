package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.api.read.MeasurementReadResponseMapper;
import at.pegelhub.measurement.api.read.input.MeasurementReadParameters;
import at.pegelhub.measurement.api.read.input.MeasurementIntervalParameters;
import at.pegelhub.measurement.api.read.output.MeasurementIntervalListResponse;
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
            MeasurementReadParameters parameters) {
        return MeasurementReadResponseMapper.toResponse(measurementService.listMeasurements(
                queryResolver.resolveList(timeSeriesId, parameters)));
    }

    @Override
    public MeasurementIntervalListResponse listMeasurementIntervals(
            UUID timeSeriesId,
            MeasurementIntervalParameters parameters) {
        return MeasurementReadResponseMapper.toResponse(measurementService.listMeasurementIntervals(
                queryResolver.resolveIntervals(timeSeriesId, parameters)));
    }

    @Override
    public Instant getSystemTime() {
        return measurementService.getSystemTime();
    }
}
