import {
  MonitoringMeasuringPointDto,
  MonitoringStationOwnerDto,
  MonitoringStationSummaryDto,
  MonitoringTimeSeriesDetailDto,
} from '../../../core/api/monitoring.dto';
import { formatMeasurementNumber } from '../../../core/measurement/measurement-format';
import {
  observedPropertyLabel,
  observedPropertyUnit,
} from '../../../core/time-series/parameter-legend';
import { waterLevelReferenceLabel } from './water-level-reference';

export interface TimeSeriesDetailFact {
  label: string;
  value: string;
}

export function timeSeriesHeadingContext(
  station: MonitoringStationSummaryDto,
  measuringPointName: string,
  measurementTypeLabel: string,
): string {
  const stationName = sameName(station.name, measuringPointName) ? null : station.name;
  const stationContext = [stationName, station.waterBody].filter(Boolean).join(' · ');
  return [measurementTypeLabel, stationContext].filter(Boolean).join(' · ');
}

export function measuringPointFacts(
  point: MonitoringMeasuringPointDto,
  owner: MonitoringStationOwnerDto,
): TimeSeriesDetailFact[] {
  const references = point.waterLevelReferences;
  return presentFacts([
    { label: 'Organisation', value: owner.shortName || owner.name },
    { label: 'Ufer', value: formatBank(point.position?.bank ?? null) },
    {
      label: 'Stromkilometer',
      value: formatWithUnit(point.position?.riverKilometer ?? null, 'km'),
    },
    { label: 'PNP', value: formatWithUnit(point.gaugeZeroElevationMAboveAdria, 'm ü. A.') },
    {
      label: waterLevelReferenceLabel('RNW', references?.referenceSetYear),
      value: formatWithUnit(references?.rnwCm ?? null, 'cm'),
    },
    {
      label: waterLevelReferenceLabel('MW', references?.referenceSetYear),
      value: formatWithUnit(references?.mwCm ?? null, 'cm'),
    },
    {
      label: waterLevelReferenceLabel('HSW', references?.referenceSetYear),
      value: formatWithUnit(references?.hswCm ?? null, 'cm'),
    },
    {
      label: waterLevelReferenceLabel('HW 100', references?.referenceSetYear),
      value: formatWithUnit(references?.hw100Cm ?? null, 'cm'),
    },
  ]);
}

export function timeSeriesFacts(series: MonitoringTimeSeriesDetailDto): TimeSeriesDetailFact[] {
  return presentFacts([
    { label: 'Einheit', value: observedPropertyUnit(series.observedProperty, series.unit) },
  ]);
}

export function measurementTypeLabel(series: MonitoringTimeSeriesDetailDto | null): string {
  return series ? observedPropertyLabel(series.observedProperty) : '';
}

function sameName(left: string, right: string): boolean {
  return left.trim().localeCompare(right.trim(), 'de-AT', { sensitivity: 'base' }) === 0;
}

function formatBank(bank: 'left' | 'right' | null): string | null {
  return bank === 'left' ? 'links' : bank === 'right' ? 'rechts' : null;
}

function formatWithUnit(value: number | null, unit: string | null): string | null {
  if (value === null) return null;
  const formatted = formatMeasurementNumber(value);
  return unit ? `${formatted} ${unit}` : formatted;
}

function presentFacts(
  facts: Array<{ label: string; value: string | null | undefined }>,
): TimeSeriesDetailFact[] {
  return facts.filter(
    (fact): fact is TimeSeriesDetailFact =>
      typeof fact.value === 'string' && fact.value.trim().length > 0,
  );
}
