import { MonitoringTimeSeriesSummaryDto } from '../../../core/api/monitoring.dto';
import {
  formatMeasurementTimestamp,
  formatMeasurementValue,
  formatRelativeMeasurementAge,
  UI_LOCALE,
} from '../../../core/measurement/measurement-format';
import { observedPropertyLabel } from '../../../core/time-series/parameter-legend';

const PARAMETER_ORDER = new Map([
  ['water-level', 0],
  ['water-temperature', 1],
  ['discharge', 2],
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
  items: readonly MonitoringTimeSeriesSummaryDto[],
): TimeSeriesOverviewView[] {
  return items
    .map((item) => ({
      id: item.id,
      measurementTypeLabel: observedPropertyLabel(item.observedProperty),
      measuringPointName: item.measuringPoint.name,
      stationLabel: stationLabel(item.measuringPoint.name, item.station),
      latestMeasurement: latestMeasurementView(item.unit, item.latestMeasurement),
      sortKey: {
        stationName: item.station.name,
        pointName: item.measuringPoint.name,
        parameterRank: parameterRank(item.observedProperty),
      },
    }))
    .sort(compareOverviewRows)
    .map(({ sortKey: _sortKey, ...row }) => row);
}

function stationLabel(
  pointName: string,
  station: MonitoringTimeSeriesSummaryDto['station'],
): string {
  const context = [station.stationNumber, station.waterBody]
    .filter((value): value is string => Boolean(value?.trim()))
    .join(' · ');

  return normalize(pointName) === normalize(station.name)
    ? context || station.name
    : [station.name, context].filter(Boolean).join(' · ');
}

function latestMeasurementView(
  unit: string,
  latest: MonitoringTimeSeriesSummaryDto['latestMeasurement'],
): TimeSeriesLatestMeasurementView {
  if (latest) {
    const timestamp = formatMeasurementTimestamp(latest.observedAt);

    return {
      value: latest.value,
      valueLabel: formatMeasurementValue(latest.value, unit),
      observedAt: latest.observedAt,
      timestamp,
      activityLabel: formatRelativeMeasurementAge(latest.observedAt) ?? timestamp,
    };
  }

  return {
    value: null,
    valueLabel: 'Kein Messwert',
    observedAt: null,
    timestamp: null,
    activityLabel: 'Keine Aktivität',
  };
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

function parameterRank(observedProperty: string): number {
  return PARAMETER_ORDER.get(observedProperty) ?? Number.MAX_SAFE_INTEGER;
}

function normalize(value: string): string {
  return value.trim().toLocaleLowerCase(UI_LOCALE);
}

interface OverviewSortKey {
  stationName: string;
  pointName: string;
  parameterRank: number;
}
