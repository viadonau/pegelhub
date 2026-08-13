import { describe, expect, it } from 'vitest';

import { MeasuringPointDto } from '../../../core/api/measuring-point.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import { waterLevelChartReferences } from './water-level-reference';

const measuringPoint: MeasuringPointDto = {
  id: 'point-1',
  stationId: 'station-1',
  name: 'Hauptpegel',
  referenceYear: 2020,
  rnw: 162,
  hsw: 480,
};

const waterLevel: TimeSeriesDto = {
  id: 'series-1',
  measuringPointId: measuringPoint.id,
  observedProperty: 'water-level',
  unit: 'cm',
};

describe('water-level chart references', () => {
  it('maps RNW and HSW into labeled chart references', () => {
    expect(waterLevelChartReferences(measuringPoint, waterLevel)).toEqual([
      { label: 'RNW 2020', value: 162, tone: 'lower' },
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
  });

  it('omits unavailable reference values', () => {
    expect(waterLevelChartReferences({ ...measuringPoint, rnw: null }, waterLevel)).toEqual([
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
  });

  it('does not place water-level references on another measurement type', () => {
    expect(
      waterLevelChartReferences(
        { ...measuringPoint },
        { ...waterLevel, observedProperty: 'discharge', unit: 'm³/s' },
      ),
    ).toEqual([]);
  });
});
