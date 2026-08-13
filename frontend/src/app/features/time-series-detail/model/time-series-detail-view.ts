import { MeasuringPointDto } from '../../../core/api/measuring-point.dto';
import { StationDto, StationOwnerDto } from '../../../core/api/station.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import { detectParameterCode } from '../../../core/time-series/parameter-legend';
import { waterLevelReferenceLabel } from './water-level-reference';

export interface TimeSeriesDetailFact {
  label: string;
  value: string;
}

const UI_LOCALE = 'de-AT';

export function measuringPointContext(station: StationDto, measuringPointName: string): string {
  const stationName = sameName(station.name, measuringPointName) ? null : station.name;

  return [stationName, station.stationNumber, station.waterBody]
    .filter((value): value is string => Boolean(value?.trim()))
    .join(' · ');
}

export function timeSeriesHeadingContext(
  station: StationDto,
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

export function measuringPointFacts(
  measuringPoint: MeasuringPointDto,
  owner: StationOwnerDto | undefined,
  referenceUnit: string | null,
): TimeSeriesDetailFact[] {
  const referenceYear = measuringPoint.referenceYear;

  return presentFacts([
    { label: 'Organisation', value: owner ? owner.shortName || owner.name : null },
    { label: 'Ufer', value: formatBank(measuringPoint.bank) },
    {
      label: 'Stromkilometer',
      value: formatWithUnit(measuringPoint.riverKilometer, 'km'),
    },
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

export function timeSeriesContextFacts(timeSeries: TimeSeriesDto): TimeSeriesDetailFact[] {
  return presentFacts([
    { label: 'Einheit', value: timeSeries.unit },
    { label: 'Externer Schlüssel', value: timeSeries.externalCode },
  ]);
}

export function referenceValueUnit(timeSeries: TimeSeriesDto | null): string | null {
  return timeSeries && detectParameterCode(timeSeries.observedProperty) === 'W'
    ? timeSeries.unit || null
    : null;
}

function presentFacts(
  facts: Array<{ label: string; value: string | null | undefined }>,
): TimeSeriesDetailFact[] {
  return facts.filter(
    (fact): fact is TimeSeriesDetailFact =>
      fact.value !== undefined && fact.value !== null && fact.value.trim().length > 0,
  );
}

function formatBank(bank: string | null | undefined): string | null {
  if (!bank?.trim()) {
    return null;
  }

  const normalized = bank.trim().toLocaleLowerCase(UI_LOCALE);

  if (normalized === 'l' || normalized === 'li' || normalized === 'left') {
    return 'links';
  }

  if (normalized === 'r' || normalized === 're' || normalized === 'right') {
    return 'rechts';
  }

  return bank.trim();
}

function formatWithUnit(value: number | null | undefined, unit: string | null): string | null {
  if (value === undefined || value === null) {
    return null;
  }

  return unit ? `${formatNumber(value)} ${unit}` : formatNumber(value);
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(UI_LOCALE, { maximumFractionDigits: 3 }).format(value);
}
