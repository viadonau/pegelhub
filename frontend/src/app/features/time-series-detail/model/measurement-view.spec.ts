import { describe, expect, it } from 'vitest';

import { MonitoringLatestMeasurementDto } from '../../../core/api/monitoring.dto';
import { latestMeasurementView } from './measurement-view';

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
});
