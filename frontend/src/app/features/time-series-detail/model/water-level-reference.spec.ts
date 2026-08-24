import { describe, expect, it } from 'vitest';

import { MonitoringMeasuringPointDto } from '../../../core/api/monitoring.dto';
import { waterLevelChartReferences, waterLevelReferenceLabel } from './water-level-reference';

function pointWithReferences(
  waterLevelReferences: MonitoringMeasuringPointDto['waterLevelReferences'],
): MonitoringMeasuringPointDto {
  return {
    id: 'point-gauge',
    name: 'Hauptpegel',
    status: 'active',
    position: null,
    gaugeZeroElevationMAboveAdria: null,
    waterLevelReferences,
  };
}

describe('water-level reference presentation', () => {
  it('projects finite chart references with their year and tone', () => {
    expect(
      waterLevelChartReferences(
        pointWithReferences({
          referenceSetYear: 2020,
          rnwCm: 162,
          mwCm: 315,
          hswCm: 480,
          hw100Cm: 590,
        }),
      ),
    ).toEqual([
      { label: 'RNW 2020', value: 162, tone: 'lower' },
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
  });

  it('omits missing and non-finite references', () => {
    expect(
      waterLevelChartReferences(
        pointWithReferences({
          referenceSetYear: 2024,
          rnwCm: Number.NaN,
          mwCm: null,
          hswCm: Number.POSITIVE_INFINITY,
          hw100Cm: null,
        }),
      ),
    ).toEqual([]);
    expect(waterLevelChartReferences(pointWithReferences(null))).toEqual([]);
  });

  it('adds a year only when one is supplied', () => {
    expect(waterLevelReferenceLabel('RNW', 2020)).toBe('RNW 2020');
    expect(waterLevelReferenceLabel('RNW', null)).toBe('RNW');
    expect(waterLevelReferenceLabel('RNW', undefined)).toBe('RNW');
  });
});
