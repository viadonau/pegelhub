import {
  MeasurementBucketListDto,
  MeasurementBucketPointDto,
} from '../app/core/api/measurement.dto';
import {
  MonitoringTimeSeriesCollectionDto,
  MonitoringTimeSeriesDetailDto,
} from '../app/core/api/monitoring.dto';
import { RuntimeConfig } from '../app/core/config/runtime-config';

export const TEST_RUNTIME_CONFIG: RuntimeConfig = {
  apiBaseUrl: '/api/v1',
  keycloak: {
    url: 'http://keycloak.test',
    realm: 'pegelhub',
    clientId: 'pegelhub-frontend',
  },
};

export function monitoringCollectionFixture(): MonitoringTimeSeriesCollectionDto {
  return {
    items: [
      {
        id: 'series-water-level',
        observedProperty: 'water-level',
        unit: 'cm',
        measuringPoint: { id: 'point-gauge', name: 'Hauptpegel' },
        station: { id: 'station-1', name: 'Wien Brigittenau', waterBody: 'Donau' },
        latestMeasurement: { observedAt: '2026-07-22T10:00:00Z', value: 312.5 },
      },
      {
        id: 'series-discharge',
        observedProperty: 'discharge',
        unit: 'm3/s',
        measuringPoint: { id: 'point-flow', name: 'Durchflussmesser' },
        station: { id: 'station-2', name: 'Korneuburg', waterBody: 'Donau' },
        latestMeasurement: null,
      },
    ],
  };
}

export function waterLevelDetailFixture(
  latestMeasurement: MonitoringTimeSeriesDetailDto['latestMeasurement'] = {
    observedAt: '2026-07-22T10:00:00Z',
    value: 312.5,
  },
): MonitoringTimeSeriesDetailDto {
  return {
    id: 'series-water-level',
    observedProperty: 'water-level',
    unit: 'cm',
    status: 'inactive',
    measuringPoint: {
      id: 'point-gauge',
      name: 'Hauptpegel',
      status: 'active',
      position: { riverKilometer: 1934.1, bank: 'right' },
      gaugeZeroElevationMAboveAdria: 156.75,
      waterLevelReferences: {
        referenceSetYear: 2020,
        rnwCm: 162,
        mwCm: 315.5,
        hswCm: 480,
        hw100Cm: null,
      },
    },
    station: { id: 'station-1', name: 'Wien Brigittenau', waterBody: 'Donau' },
    stationOwner: { id: 'owner-1', name: 'viadonau', shortName: 'viadonau' },
    latestMeasurement,
  };
}

export function measurementBucketsFixture(
  points: MeasurementBucketPointDto[] = [],
): MeasurementBucketListDto {
  return {
    timeSeriesId: 'series-water-level',
    window: null,
    resolution: null,
    points,
  };
}
