import { describe, expect, it } from 'vitest';

import {
  MonitoringMeasuringPointDto,
  MonitoringStationOwnerDto,
  MonitoringStationSummaryDto,
  MonitoringTimeSeriesDetailDto,
} from '../../../core/api/monitoring.dto';
import { timeSeriesDetailView } from './time-series-detail-view';

const station: MonitoringStationSummaryDto = {
  id: 'station-1',
  stationNumber: 'AT-001',
  name: 'Kienstock',
  waterBody: 'Donau',
};
const owner: MonitoringStationOwnerDto = {
  id: 'owner-1',
  name: 'via donau - Oesterreichische Wasserstrassengesellschaft',
  shortName: 'viadonau',
};
const measuringPoint: MonitoringMeasuringPointDto = {
  id: 'point-1',
  name: 'Hauptpegel',
  bank: 'right',
  riverKilometer: 2015.4,
  referenceLevel: 142.75,
  referenceYear: 2020,
  rnw: null,
  mw: 315.5,
  hsw: null,
  hw100: null,
};

const detail = (observedProperty: string, unit: string): MonitoringTimeSeriesDetailDto => ({
  id: 'series-1',
  observedProperty,
  unit,
  externalCode: observedProperty === 'water-temperature' ? 'WT-001' : null,
  measuringPoint,
  station,
  stationOwner: owner,
  latestMeasurement: null,
});

describe('time-series detail view', () => {
  it('keeps point identity and stable metadata separate from its station', () => {
    const view = timeSeriesDetailView(detail('water-level', 'cm'));

    expect(view.headingContext).toBe('Wasserstand · Kienstock · AT-001 · Donau');
    expect(view.pointFacts).toEqual([
      { label: 'Organisation', value: 'viadonau' },
      { label: 'Ufer', value: 'rechts' },
      { label: 'Stromkilometer', value: '2 015,4 km' },
      { label: 'PNP', value: '142,75 m ü. A.' },
      { label: 'MW 2020', value: '315,5 cm' },
    ]);

    const sameNameView = timeSeriesDetailView({
      ...detail('water-level', 'cm'),
      measuringPoint: { ...measuringPoint, name: station.name },
    });
    expect(sameNameView.headingContext).toBe('Wasserstand · AT-001 · Donau');
  });

  it('uses the active water-level unit for point reference values', () => {
    const levelView = timeSeriesDetailView(detail('water-level', 'cm'));
    const temperatureView = timeSeriesDetailView(detail('water-temperature', '°C'));

    expect(levelView.pointFacts).toContainEqual({
      label: 'MW 2020',
      value: '315,5 cm',
    });
    expect(temperatureView.pointFacts).toContainEqual({ label: 'MW 2020', value: '315,5' });
    expect(temperatureView.seriesFacts).toEqual([
      { label: 'Einheit', value: '°C' },
      { label: 'Externer Schlüssel', value: 'WT-001' },
    ]);
  });
});
