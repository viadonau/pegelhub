import { describe, expect, it } from 'vitest';

import {
  MonitoringMeasuringPointDto,
  MonitoringTimeSeriesDetailDto,
} from '../../../core/api/monitoring.dto';
import { waterLevelChartReferences } from './water-level-reference';

const measuringPoint: MonitoringMeasuringPointDto = {
  id: 'point-1',
  name: 'Hauptpegel',
  referenceYear: 2020,
  referenceLevel: null,
  riverKilometer: null,
  bank: null,
  rnw: 162,
  mw: null,
  hsw: 480,
  hw100: null,
};

const detail = (observedProperty: string): MonitoringTimeSeriesDetailDto => ({
  id: 'series-1',
  observedProperty,
  unit: 'cm',
  externalCode: null,
  measuringPoint,
  station: { id: 'station-1', stationNumber: '1', name: 'Station', waterBody: 'Donau' },
  stationOwner: { id: 'owner-1', name: 'Owner', shortName: null },
  latestMeasurement: null,
});

describe('water-level chart references', () => {
  it('maps RNW and HSW into labeled chart references', () => {
    expect(waterLevelChartReferences(measuringPoint, detail('water-level'))).toEqual([
      { label: 'RNW 2020', value: 162, tone: 'lower' },
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
  });

  it('does not classify another property as water level', () => {
    expect(waterLevelChartReferences(measuringPoint, detail('level'))).toEqual([]);
  });
});
