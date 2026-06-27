package at.pegelhub.measurement.api.read;

import at.pegelhub.measurement.api.read.output.MeasurementAggregation;
import at.pegelhub.measurement.api.read.output.MeasurementBucketListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementBucketPointResponse;
import at.pegelhub.measurement.api.read.output.MeasurementListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementNextPageResponse;
import at.pegelhub.measurement.api.read.output.MeasurementPointResponse;
import at.pegelhub.measurement.api.read.output.MeasurementResolutionResponse;
import at.pegelhub.measurement.api.read.output.MeasurementSortOrder;
import at.pegelhub.measurement.api.read.output.MeasurementWindowResponse;
import at.pegelhub.measurement.application.MeasurementBucketList;
import at.pegelhub.measurement.application.MeasurementList;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementPageRow;
import at.pegelhub.measurement.domain.MeasurementBucket;

import java.time.Instant;

public final class MeasurementReadResponseMapper {

    private static final MeasurementCursorCodec CURSOR_CODEC = new MeasurementCursorCodec();

    private MeasurementReadResponseMapper() {
    }

    public static MeasurementListResponse toResponse(MeasurementList list) {
        return new MeasurementListResponse(
                list.query().timeSeriesId().value(),
                toWindowResponse(list.query().window()),
                toResponseOrder(list.query().order()),
                list.query().limit(),
                list.truncated(),
                nextPage(list),
                list.measurements().stream()
                        .map(MeasurementReadResponseMapper::toPointResponse)
                        .toList());
    }

    public static MeasurementBucketListResponse toResponse(MeasurementBucketList list) {
        return new MeasurementBucketListResponse(
                list.query().timeSeriesId().value(),
                toWindowResponse(list.query().window()),
                new MeasurementResolutionResponse(
                        list.query().resolution().bucketWidth().toString(),
                        MeasurementAggregation.AVERAGE,
                        list.query().resolution().targetPointCount()),
                list.buckets().stream()
                        .map(MeasurementReadResponseMapper::toBucketPointResponse)
                        .toList());
    }

    private static MeasurementWindowResponse toWindowResponse(at.pegelhub.measurement.application.MeasurementWindow window) {
        return new MeasurementWindowResponse(window.from(), window.to(), window.requested());
    }

    private static MeasurementPointResponse toPointResponse(MeasurementPageRow measurement) {
        return new MeasurementPointResponse(
                measurement.observedAt(),
                measurement.value());
    }

    private static MeasurementBucketPointResponse toBucketPointResponse(MeasurementBucket bucket) {
        return new MeasurementBucketPointResponse(
                bucket.from(),
                bucket.to(),
                bucket.value(),
                bucket.sampleCount());
    }

    private static MeasurementSortOrder toResponseOrder(MeasurementOrder order) {
        return switch (order) {
            case ASC -> MeasurementSortOrder.ASC;
            case DESC -> MeasurementSortOrder.DESC;
        };
    }

    private static MeasurementNextPageResponse nextPage(MeasurementList list) {
        if (list.nextCursor() == null) {
            return null;
        }
        return new MeasurementNextPageResponse(
                list.query().window().from(),
                list.query().window().to(),
                toResponseOrder(list.query().order()),
                list.query().limit(),
                CURSOR_CODEC.encode(list.nextCursor()));
    }

}
