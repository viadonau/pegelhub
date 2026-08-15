import {
  MonitoringMeasuringPointDto,
  MonitoringStationOwnerDto,
  MonitoringStationSummaryDto,
  MonitoringTimeSeriesDetailDto,
} from '../../../core/api/monitoring.dto';
import { formatMeasurementNumber, UI_LOCALE } from '../../../core/measurement/measurement-format';
import { observedPropertyLabel } from '../../../core/time-series/parameter-legend';
import {
  WaterLevelReference,
  waterLevelChartReferences,
  waterLevelReferenceLabel,
} from './water-level-reference';

export interface TimeSeriesDetailFact {
  label: string;
  value: string;
}

export interface TimeSeriesDetailView {
  timeSeries: MonitoringTimeSeriesDetailDto | null;
  measuringPoint: MonitoringMeasuringPointDto | null;
  station: MonitoringStationSummaryDto | null;
  stationOwner: MonitoringStationOwnerDto | null;
  measurementTypeLabel: string;
  headingContext: string;
  pointFacts: readonly TimeSeriesDetailFact[];
  seriesFacts: readonly TimeSeriesDetailFact[];
  referenceLines: readonly WaterLevelReference[];
}

export function timeSeriesDetailView(
  snapshot: MonitoringTimeSeriesDetailDto | null,
): TimeSeriesDetailView {
  const measurementTypeLabel = snapshot ? observedPropertyLabel(snapshot.observedProperty) : '';
  const measuringPoint = snapshot?.measuringPoint ?? null;
  const station = snapshot?.station ?? null;
  const stationOwner = snapshot?.stationOwner ?? null;
  const headingContext =
    measuringPoint && station
      ? timeSeriesHeadingContext(station, measuringPoint.name, measurementTypeLabel)
      : measurementTypeLabel;

  return {
    timeSeries: snapshot,
    measuringPoint,
    station,
    stationOwner,
    measurementTypeLabel,
    headingContext,
    pointFacts: measuringPoint
      ? measuringPointFacts(measuringPoint, stationOwner, referenceValueUnit(snapshot))
      : [],
    seriesFacts: snapshot ? timeSeriesContextFacts(snapshot) : [],
    referenceLines:
      snapshot && measuringPoint ? waterLevelChartReferences(measuringPoint, snapshot) : [],
  };
}

function measuringPointContext(
  station: MonitoringStationSummaryDto,
  measuringPointName: string,
): string {
  const stationName = sameName(station.name, measuringPointName) ? null : station.name;

  return [stationName, station.stationNumber, station.waterBody]
    .filter((value): value is string => Boolean(value?.trim()))
    .join(' · ');
}

function timeSeriesHeadingContext(
  station: MonitoringStationSummaryDto,
  measuringPointName: string,
  measurementTypeLabel: string,
): string {
  return [measurementTypeLabel, measuringPointContext(station, measuringPointName)]
    .filter(Boolean)
    .join(' · ');
}

function sameName(left: string, right: string): boolean {
  return left.trim().localeCompare(right.trim(), UI_LOCALE, { sensitivity: 'base' }) === 0;
}

function measuringPointFacts(
  measuringPoint: MonitoringMeasuringPointDto,
  owner: MonitoringStationOwnerDto | null,
  referenceUnit: string | null,
): TimeSeriesDetailFact[] {
  const referenceYear = measuringPoint.referenceYear;

  return presentFacts([
    { label: 'Organisation', value: owner ? owner.shortName || owner.name : null },
    { label: 'Ufer', value: formatBank(measuringPoint.bank) },
    { label: 'Stromkilometer', value: formatWithUnit(measuringPoint.riverKilometer, 'km') },
    { label: 'PNP', value: formatWithUnit(measuringPoint.referenceLevel, 'm ü. A.') },
    {
      label: waterLevelReferenceLabel('RNW', referenceYear),
      value: formatWithUnit(measuringPoint.rnw, referenceUnit),
    },
    {
      label: waterLevelReferenceLabel('MW', referenceYear),
      value: formatWithUnit(measuringPoint.mw, referenceUnit),
    },
    {
      label: waterLevelReferenceLabel('HSW', referenceYear),
      value: formatWithUnit(measuringPoint.hsw, referenceUnit),
    },
    { label: 'HW 100', value: formatWithUnit(measuringPoint.hw100, referenceUnit) },
  ]);
}

function timeSeriesContextFacts(timeSeries: MonitoringTimeSeriesDetailDto): TimeSeriesDetailFact[] {
  return presentFacts([
    { label: 'Einheit', value: timeSeries.unit },
    { label: 'Externer Schlüssel', value: timeSeries.externalCode },
  ]);
}

function referenceValueUnit(timeSeries: MonitoringTimeSeriesDetailDto | null): string | null {
  return timeSeries?.observedProperty === 'water-level' ? timeSeries.unit || null : null;
}

function presentFacts(
  facts: Array<{ label: string; value: string | null | undefined }>,
): TimeSeriesDetailFact[] {
  return facts.filter(
    (fact): fact is TimeSeriesDetailFact =>
      fact.value !== undefined && fact.value !== null && fact.value.trim().length > 0,
  );
}

function formatBank(bank: 'left' | 'right' | null): string | null {
  return bank === 'left' ? 'links' : bank === 'right' ? 'rechts' : null;
}

function formatWithUnit(value: number | null | undefined, unit: string | null): string | null {
  if (value === undefined || value === null) {
    return null;
  }

  const formatted = formatMeasurementNumber(value);

  return unit ? `${formatted} ${unit}` : formatted;
}
