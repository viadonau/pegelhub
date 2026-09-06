import { describe, expect, it } from 'vitest';

import { MonitoringTimeSeriesDetailDto } from '../../../core/api/monitoring.dto';
import { waterLevelDetailFixture } from '../../../../testing/fixtures';
import {
  measuringPointFacts,
  measurementTypeLabel,
  timeSeriesFacts,
  timeSeriesHeadingContext,
} from './detail-projection';

describe('time-series detail projection', () => {
  it('presents full operator metadata with canonical labels and units', () => {
    const series = waterLevelDetailFixture();

    expect(
      timeSeriesHeadingContext(
        series.station,
        series.measuringPoint.name,
        measurementTypeLabel(series),
      ),
    ).toBe('Wasserstand · Wien Brigittenau · Donau');
    expect(measuringPointFacts(series.measuringPoint, series.stationOwner)).toEqual([
      { label: 'Organisation', value: 'viadonau' },
      { label: 'Ufer', value: 'rechts' },
      { label: 'Stromkilometer', value: '1\u00a0934,1 km' },
      { label: 'PNP', value: '156,75 m ü. A.' },
      { label: 'RNW 2020', value: '162 cm' },
      { label: 'MW 2020', value: '315,5 cm' },
      { label: 'HSW 2020', value: '480 cm' },
    ]);
    expect(timeSeriesFacts({ ...series, unit: 'centimetre' })).toEqual([
      { label: 'Einheit', value: 'cm' },
    ]);
  });

  it('omits absent facts, falls back to owner name, and labels the left bank', () => {
    const sparse: MonitoringTimeSeriesDetailDto = {
      id: 'series-sparse',
      observedProperty: 'conductivity',
      unit: 'µS/cm',
      status: 'active',
      measuringPoint: {
        id: 'point-sparse',
        name: 'Messpunkt Nord',
        status: 'active',
        position: { riverKilometer: null, bank: 'left' },
        gaugeZeroElevationMAboveAdria: null,
        waterLevelReferences: null,
      },
      station: { id: 'station-sparse', name: 'Messpunkt Nord', waterBody: '' },
      stationOwner: { id: 'owner-sparse', name: 'Hydro Betrieb', shortName: null },
      latestMeasurement: null,
    };

    expect(measuringPointFacts(sparse.measuringPoint, sparse.stationOwner)).toEqual([
      { label: 'Organisation', value: 'Hydro Betrieb' },
      { label: 'Ufer', value: 'links' },
    ]);
    expect(timeSeriesFacts(sparse)).toEqual([{ label: 'Einheit', value: 'µS/cm' }]);
    expect(
      timeSeriesHeadingContext(sparse.station, sparse.measuringPoint.name, 'Leitfähigkeit'),
    ).toBe('Leitfähigkeit');
  });
});
