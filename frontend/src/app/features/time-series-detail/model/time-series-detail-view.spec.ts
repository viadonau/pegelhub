import { describe, expect, it } from 'vitest';

import { MeasuringPointDto } from '../../../core/api/measuring-point.dto';
import { StationDto, StationOwnerDto } from '../../../core/api/station.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import {
  measuringPointContext,
  measuringPointFacts,
  referenceValueUnit,
  timeSeriesHeadingContext,
  timeSeriesContextFacts,
} from './time-series-detail-view';

const station: StationDto = {
  id: 'station-1',
  ownerId: 'owner-1',
  stationNumber: 'AT-001',
  name: 'Kienstock',
  waterBody: 'Donau',
  location: 'Wachau',
};

const owner: StationOwnerDto = {
  id: 'owner-1',
  name: 'via donau - Oesterreichische Wasserstrassengesellschaft',
  shortName: 'viadonau',
};

const measuringPoint: MeasuringPointDto = {
  id: 'point-1',
  stationId: station.id,
  name: 'Hauptpegel',
  bank: 're',
  riverKilometer: 2015.4,
  referenceLevel: 142.75,
  referenceYear: 2020,
  mw: 315.5,
};

describe('time-series detail view', () => {
  it('keeps point identity and stable metadata separate from its station', () => {
    expect(measuringPointContext(station, measuringPoint.name)).toBe('Kienstock · AT-001 · Donau');
    expect(measuringPointContext(station, station.name)).toBe('AT-001 · Donau');
    expect(timeSeriesHeadingContext(station, measuringPoint.name, 'Wasserstand')).toBe(
      'Wasserstand · Kienstock · AT-001 · Donau',
    );
    expect(measuringPointFacts(measuringPoint, owner, 'cm')).toEqual([
      { label: 'Organisation', value: 'viadonau' },
      { label: 'Ufer', value: 'rechts' },
      { label: 'Stromkilometer', value: '2 015,4 km' },
      { label: 'PNP', value: '142,75 m ü. A.' },
      { label: 'MW 2020', value: '315,5 cm' },
    ]);
  });

  it('uses the active water-level unit for point reference values', () => {
    const level = series('level', 'water-level', 'cm');
    const temperature = {
      ...series('temperature', 'water-temperature', '°C'),
      externalCode: 'WT-001',
    };
    const unit = referenceValueUnit(level);

    expect(unit).toBe('cm');
    expect(measuringPointFacts(measuringPoint, owner, unit)).toContainEqual({
      label: 'MW 2020',
      value: '315,5 cm',
    });
    expect(timeSeriesContextFacts(temperature)).toEqual([
      { label: 'Einheit', value: '°C' },
      { label: 'Externer Schlüssel', value: 'WT-001' },
    ]);
    expect(referenceValueUnit(temperature)).toBeNull();
  });
});

function series(id: string, observedProperty: string, unit: string): TimeSeriesDto {
  return {
    id,
    measuringPointId: measuringPoint.id,
    observedProperty,
    unit,
  };
}
