import { describe, expect, it } from 'vitest';

import { MeasurementBucketListDto } from '../../../core/api/measurement.dto';
import { MonitoringLatestMeasurementDto } from '../../../core/api/monitoring.dto';
import { latestMeasurementView, measurementChartSeries } from './measurement-view';

describe('measurement view', () => {
  it('formats the latest value from the monitoring snapshot', () => {
    const latest: MonitoringLatestMeasurementDto = {
      observedAt: '2026-07-19T10:00:00Z',
      value: 305.5,
    };

    expect(latestMeasurementView(latest, 'cm')).toMatchObject({
      unit: 'cm',
      value: '305,5',
    });
    expect(latestMeasurementView(null, 'cm')).toBeNull();
  });

  it('maps backend-ordered chart buckets without reordering them', () => {
    const buckets: MeasurementBucketListDto = {
      timeSeriesId: 'series-1',
      window: null,
      resolution: null,
      points: [
        { from: '2026-07-19T08:00:00Z', to: '2026-07-19T09:00:00Z', value: 301, sampleCount: 2 },
        { from: '2026-07-19T10:00:00Z', to: '2026-07-19T11:00:00Z', value: 305, sampleCount: 2 },
      ],
    };

    expect(
      measurementChartSeries(buckets, 'water-level')?.points.map((point) => point.value),
    ).toEqual([301, 305]);
  });
});
