import { describe, expect, it } from 'vitest';

import {
  EMPTY_MEASUREMENT_BUCKET_LIST,
  EMPTY_MEASUREMENT_LIST,
  MeasurementBucketListDto,
  MeasurementListDto,
} from '../../../core/api/measurement.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';

import { latestMeasurementView, measurementChartSeries } from './measurement-view';

const timeSeries: TimeSeriesDto = {
  id: 'series-1',
  measuringPointId: 'point-1',
  observedProperty: 'water-level',
  unit: 'cm',
};

describe('measurement view', () => {
  it('formats the first reading from the descending latest-value response', () => {
    const response: MeasurementListDto = {
      ...EMPTY_MEASUREMENT_LIST,
      timeSeriesId: timeSeries.id,
      measurements: [
        { observedAt: '2026-07-19T10:00:00Z', value: 305.5 },
        { observedAt: '2026-07-19T08:00:00Z', value: 301.25 },
      ],
    };

    expect(latestMeasurementView(response, timeSeries)).toMatchObject({
      unit: 'cm',
      value: '305,5',
    });
    expect(latestMeasurementView(EMPTY_MEASUREMENT_LIST, timeSeries)).toBeNull();
  });

  it('maps backend-ordered chart buckets without reordering them', () => {
    const buckets: MeasurementBucketListDto = {
      ...EMPTY_MEASUREMENT_BUCKET_LIST,
      timeSeriesId: timeSeries.id,
      points: [
        {
          from: '2026-07-19T08:00:00Z',
          to: '2026-07-19T09:00:00Z',
          value: 301,
          sampleCount: 2,
        },
        {
          from: '2026-07-19T10:00:00Z',
          to: '2026-07-19T11:00:00Z',
          value: 305,
          sampleCount: 2,
        },
      ],
    };

    expect(measurementChartSeries(buckets, timeSeries)?.points.map((point) => point.value)).toEqual(
      [301, 305],
    );
  });
});
