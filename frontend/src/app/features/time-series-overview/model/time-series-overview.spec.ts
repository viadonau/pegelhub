import { describe, expect, it } from 'vitest';

import { MonitoringTimeSeriesSummaryDto } from '../../../core/api/monitoring.dto';
import { formatMeasurementTimestamp } from '../../../core/measurement/measurement-format';
import { timeSeriesOverviewViews } from './time-series-overview';

function item(
  id: string,
  stationName: string,
  pointName: string,
  property: string,
  latestMeasurement: MonitoringTimeSeriesSummaryDto['latestMeasurement'] = null,
): MonitoringTimeSeriesSummaryDto {
  return {
    id,
    observedProperty: property,
    unit: property === 'discharge' ? 'm3/s' : property === 'water-temperature' ? 'Cel' : 'cm',
    measuringPoint: { id: `point-${id}`, name: pointName },
    station: { id: `station-${id}`, name: stationName, waterBody: 'Donau' },
    latestMeasurement,
  };
}

describe('time-series overview projection', () => {
  it('orders stations, points, canonical properties, unknown labels, then stable IDs', () => {
    const rows = timeSeriesOverviewViews([
      item('series-z', 'Wien', 'Pegel B', 'water-level'),
      item('series-unknown-z', 'Wien', 'Pegel A', 'zeta'),
      item('series-q', 'Wien', 'Pegel A', 'discharge'),
      item('series-wt', 'Wien', 'Pegel A', 'water-temperature'),
      item('series-unknown-a-2', 'Wien', 'Pegel A', 'alpha'),
      item('series-w', 'Wien', 'Pegel A', 'water-level'),
      item('series-unknown-a-1', 'Wien', 'Pegel A', 'alpha'),
      item('series-first', 'Korneuburg', 'Pegel Z', 'discharge'),
    ]);

    expect(rows.map((row) => row.id)).toEqual([
      'series-first',
      'series-w',
      'series-wt',
      'series-q',
      'series-unknown-a-1',
      'series-unknown-a-2',
      'series-unknown-z',
      'series-z',
    ]);
  });

  it('avoids repeating a same-name station and uses canonical presentation values', () => {
    const [row] = timeSeriesOverviewViews([
      item('series-w', ' Hauptpegel ', 'hauptpegel', 'water-level', {
        observedAt: '2026-07-22T10:00:00Z',
        value: 312.5,
      }),
    ]);

    expect(row).toMatchObject({
      measurementTypeLabel: 'Wasserstand',
      stationLabel: 'Donau',
      latestMeasurement: {
        value: 312.5,
        valueLabel: '312,5 cm',
        observedAt: '2026-07-22T10:00:00Z',
        timestamp: formatMeasurementTimestamp('2026-07-22T10:00:00Z'),
      },
    });
  });

  it('keeps a real empty measurement distinct from the number zero', () => {
    const rows = timeSeriesOverviewViews([
      item('series-zero', 'Wien', 'Pegel A', 'discharge', {
        observedAt: '2026-07-22T10:00:00Z',
        value: 0,
      }),
      item('series-empty', 'Wien', 'Pegel B', 'water-temperature'),
    ]);

    expect(rows.find((row) => row.id === 'series-zero')?.latestMeasurement.valueLabel).toBe(
      '0 m³/s',
    );
    expect(rows.find((row) => row.id === 'series-empty')?.latestMeasurement).toMatchObject({
      value: null,
      valueLabel: 'Kein Messwert',
      activityLabel: 'Keine Aktivität',
    });
  });
});
