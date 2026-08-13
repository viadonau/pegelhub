import { describe, expect, it } from 'vitest';

import { MeasuringPointDto } from '../../../core/api/measuring-point.dto';
import { StationDto } from '../../../core/api/station.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import { formatMeasurementTimestamp } from '../../../core/measurement/measurement-format';
import { LatestMeasurementLookup } from './latest-measurement';
import { timeSeriesOverviewViews } from './time-series-overview';

const stations: StationDto[] = [
  station('station-wien', '10001030', 'Wien Brigittenau', 'Donau'),
  station('station-wachau', '10000001', 'Kienstock', 'Donau'),
];

const points: MeasuringPointDto[] = [
  point('point-temperature', 'station-wien', 'Temperatursonde'),
  point('point-main', 'station-wien', 'Hauptpegel'),
  point('point-wachau', 'station-wachau', 'Uferpegel'),
  point('point-empty', 'station-wien', 'Reservepunkt'),
];

const timeSeries: TimeSeriesDto[] = [
  series('series-q', 'point-main', 'discharge', 'm³/s', 'Q-001'),
  series('series-wt', 'point-main', 'water-temperature', '°C', 'WT-001'),
  series('series-w', 'point-main', 'water-level', 'cm', 'W-001'),
  series('series-c', 'point-main', 'conductivity', 'µS/cm', 'C-001'),
  series('series-temperature', 'point-temperature', 'water-temperature', '°C', 'WT-002'),
];

describe('time-series overview projection', () => {
  it('creates one row per time series and keeps the domain hierarchy as columns', () => {
    const rows = timeSeriesOverviewViews(points, stations, timeSeries, new Map());

    expect(rows.map((row) => row.id)).toEqual([
      'series-w',
      'series-wt',
      'series-q',
      'series-c',
      'series-temperature',
    ]);
    expect(rows[0]).toMatchObject({
      measurementTypeLabel: 'Wasserstand',
      measuringPointName: 'Hauptpegel',
      stationLabel: 'Wien Brigittenau · 10001030 · Donau',
    });
  });

  it('does not invent overview rows for measuring points without time series', () => {
    const rows = timeSeriesOverviewViews(points, stations, timeSeries, new Map());

    expect(rows.some((row) => row.measuringPointName === 'Reservepunkt')).toBe(false);
  });

  it('projects each latest measurement onto its own stable row', () => {
    const latest: LatestMeasurementLookup = new Map([
      [
        'series-w',
        {
          status: 'available',
          measurement: { observedAt: '2026-07-22T10:00:00Z', value: 312.5 },
        },
      ],
      ['series-wt', { status: 'empty' }],
      ['series-q', { status: 'error' }],
    ]);
    const views = timeSeriesOverviewViews(points, stations, timeSeries, latest);
    const waterLevel = views.find((row) => row.id === 'series-w');

    expect(waterLevel?.latestMeasurement).toMatchObject({
      value: 312.5,
      valueLabel: '312,5 cm',
      observedAt: '2026-07-22T10:00:00Z',
      timestamp: formatMeasurementTimestamp('2026-07-22T10:00:00Z'),
    });
    expect(views.find((row) => row.id === 'series-wt')?.latestMeasurement.valueLabel).toBe(
      'Kein Messwert',
    );
    expect(views.find((row) => row.id === 'series-q')?.latestMeasurement.valueLabel).toBe(
      'Nicht verfügbar',
    );
  });
});

function station(id: string, stationNumber: string, name: string, waterBody: string): StationDto {
  return {
    id,
    ownerId: 'owner-1',
    stationNumber,
    name,
    waterBody,
  };
}

function point(id: string, stationId: string, name: string): MeasuringPointDto {
  return { id, stationId, name };
}

function series(
  id: string,
  measuringPointId: string,
  observedProperty: string,
  unit: string,
  externalCode: string,
): TimeSeriesDto {
  return { id, measuringPointId, observedProperty, unit, externalCode };
}
