import { describe, expect, it } from 'vitest';

import { formatMeasurementTimestamp } from '../../../core/measurement/measurement-format';
import { MonitoringTimeSeriesSummaryDto } from '../../../core/api/monitoring.dto';
import { timeSeriesOverviewViews } from './time-series-overview';

const item = (
  id: string,
  property: string,
  latestMeasurement: MonitoringTimeSeriesSummaryDto['latestMeasurement'] = null,
): MonitoringTimeSeriesSummaryDto => ({
  id,
  observedProperty: property,
  unit: property === 'discharge' ? 'm3/s' : property === 'water-temperature' ? 'Cel' : 'cm',
  measuringPoint: { id: 'point-1', name: 'Hauptpegel' },
  station: {
    id: 'station-1',
    name: 'Wien Brigittenau',
    waterBody: 'Donau',
  },
  latestMeasurement,
});

describe('time-series overview projection', () => {
  it('keeps one row per backend TimeSeries item and sorts canonical properties', () => {
    const rows = timeSeriesOverviewViews([
      item('series-q', 'discharge'),
      item('series-wt', 'water-temperature'),
      item('series-w', 'water-level'),
      item('series-unknown', 'conductivity'),
    ]);

    expect(rows.map((row) => row.id)).toEqual([
      'series-w',
      'series-wt',
      'series-q',
      'series-unknown',
    ]);
    expect(rows[0]).toMatchObject({
      measurementTypeLabel: 'Wasserstand',
      stationLabel: 'Wien Brigittenau · Donau',
    });
  });

  it('projects each latest measurement without cross-series state', () => {
    const views = timeSeriesOverviewViews([
      item('series-w', 'water-level', {
        observedAt: '2026-07-22T10:00:00Z',
        value: 312.5,
      }),
      item('series-empty', 'water-temperature'),
    ]);

    const level = views.find((row) => row.id === 'series-w');
    expect(level?.latestMeasurement).toMatchObject({
      value: 312.5,
      valueLabel: '312,5 cm',
      observedAt: '2026-07-22T10:00:00Z',
      timestamp: formatMeasurementTimestamp('2026-07-22T10:00:00Z'),
    });
    expect(views.find((row) => row.id === 'series-empty')?.latestMeasurement.valueLabel).toBe(
      'Kein Messwert',
    );
  });
});
