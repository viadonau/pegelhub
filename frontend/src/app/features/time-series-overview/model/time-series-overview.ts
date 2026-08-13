import { MeasuringPointDto } from '../../../core/api/measuring-point.dto';
import { StationDto } from '../../../core/api/station.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import {
  formatMeasurementTimestamp,
  formatMeasurementValue,
  formatRelativeMeasurementAge,
} from '../../../core/measurement/measurement-format';
import {
  detectParameterCode,
  observedPropertyLabel,
} from '../../../core/time-series/parameter-legend';
import { LatestMeasurementLookup, LatestMeasurementResult } from './latest-measurement';

const UI_LOCALE = 'de-AT';
const PARAMETER_ORDER = new Map([
  ['W', 0],
  ['WT', 1],
  ['Q', 2],
]);

export interface TimeSeriesLatestMeasurementView {
  value: number | null;
  valueLabel: string;
  observedAt: string | null;
  timestamp: string | null;
  activityLabel: string;
}

export interface TimeSeriesOverviewView {
  id: string;
  measurementTypeLabel: string;
  measuringPointName: string;
  stationLabel: string;
  latestMeasurement: TimeSeriesLatestMeasurementView;
}

export function timeSeriesOverviewViews(
  measuringPoints: readonly MeasuringPointDto[],
  stations: readonly StationDto[],
  timeSeries: readonly TimeSeriesDto[],
  latestMeasurements: LatestMeasurementLookup,
): TimeSeriesOverviewView[] {
  const pointsById = new Map(measuringPoints.map((point) => [point.id, point]));
  const stationsById = new Map(stations.map((station) => [station.id, station]));

  return timeSeries
    .flatMap((series) => {
      const point = pointsById.get(series.measuringPointId);

      if (!point) {
        return [];
      }

      const station = stationsById.get(point.stationId);

      return [
        {
          id: series.id,
          measurementTypeLabel: observedPropertyLabel(series.observedProperty),
          measuringPointName: point.name,
          stationLabel: stationLabel(point.name, station),
          latestMeasurement: latestMeasurementView(
            series.unit,
            latestMeasurements.get(series.id) ?? { status: 'loading' },
          ),
          sortKey: {
            stationName: station?.name ?? '',
            pointName: point.name,
            parameterRank: parameterRank(series),
          },
        },
      ];
    })
    .sort(compareOverviewRows)
    .map(({ sortKey: _sortKey, ...row }) => row);
}

function stationLabel(pointName: string, station: StationDto | undefined): string {
  const stationName = station?.name ?? 'Unbekannte Pegelstelle';
  const context = [station?.stationNumber, station?.waterBody]
    .filter((value): value is string => Boolean(value?.trim()))
    .join(' · ');

  return normalize(pointName) === normalize(stationName)
    ? context || stationName
    : [stationName, context].filter(Boolean).join(' · ');
}

function latestMeasurementView(
  unit: string,
  latest: LatestMeasurementResult,
): TimeSeriesLatestMeasurementView {
  if (latest.status === 'available') {
    const timestamp = formatMeasurementTimestamp(latest.measurement.observedAt);

    return {
      value: latest.measurement.value,
      valueLabel: formatMeasurementValue(latest.measurement.value, unit),
      observedAt: latest.measurement.observedAt,
      timestamp,
      activityLabel: formatRelativeMeasurementAge(latest.measurement.observedAt) ?? timestamp,
    };
  }

  return {
    value: null,
    valueLabel: measurementStateLabel(latest.status),
    observedAt: null,
    timestamp: null,
    activityLabel:
      latest.status === 'empty' ? 'Keine Aktivität' : measurementStateLabel(latest.status),
  };
}

function measurementStateLabel(status: 'empty' | 'error' | 'loading'): string {
  switch (status) {
    case 'loading':
      return 'Wird geladen';
    case 'error':
      return 'Nicht verfügbar';
    case 'empty':
      return 'Kein Messwert';
  }
}

function compareOverviewRows(
  left: { id: string; measurementTypeLabel: string; sortKey: OverviewSortKey },
  right: { id: string; measurementTypeLabel: string; sortKey: OverviewSortKey },
): number {
  return (
    left.sortKey.stationName.localeCompare(right.sortKey.stationName, UI_LOCALE) ||
    left.sortKey.pointName.localeCompare(right.sortKey.pointName, UI_LOCALE) ||
    left.sortKey.parameterRank - right.sortKey.parameterRank ||
    left.measurementTypeLabel.localeCompare(right.measurementTypeLabel, UI_LOCALE) ||
    left.id.localeCompare(right.id)
  );
}

function parameterRank(timeSeries: TimeSeriesDto): number {
  const code = detectParameterCode(timeSeries.observedProperty);

  return code ? (PARAMETER_ORDER.get(code) ?? Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER;
}

function normalize(value: string): string {
  return value.trim().toLocaleLowerCase(UI_LOCALE);
}

interface OverviewSortKey {
  stationName: string;
  pointName: string;
  parameterRank: number;
}
