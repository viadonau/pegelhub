export type MonitoringStatus = 'active' | 'inactive';

export interface MonitoringLatestMeasurementDto {
  observedAt: string;
  value: number;
}

export interface MonitoringMeasuringPointSummaryDto {
  id: string;
  name: string;
}

export interface MonitoringPositionDto {
  riverKilometer: number | null;
  bank: 'left' | 'right' | null;
}

export interface MonitoringWaterLevelReferencesDto {
  referenceSetYear: number;
  rnwCm: number | null;
  mwCm: number | null;
  hswCm: number | null;
  hw100Cm: number | null;
}

export interface MonitoringMeasuringPointDto {
  id: string;
  name: string;
  status: MonitoringStatus;
  position: MonitoringPositionDto | null;
  gaugeZeroElevationMAboveAdria: number | null;
  waterLevelReferences: MonitoringWaterLevelReferencesDto | null;
}

export interface MonitoringStationSummaryDto {
  id: string;
  name: string;
  waterBody: string;
}

export interface MonitoringStationOwnerDto {
  id: string;
  name: string;
  shortName: string | null;
}

export interface MonitoringTimeSeriesSummaryDto {
  id: string;
  observedProperty: string;
  unit: string;
  measuringPoint: MonitoringMeasuringPointSummaryDto;
  station: MonitoringStationSummaryDto;
  latestMeasurement: MonitoringLatestMeasurementDto | null;
}

export interface MonitoringTimeSeriesCollectionDto {
  items: MonitoringTimeSeriesSummaryDto[];
}

export interface MonitoringTimeSeriesDetailDto {
  id: string;
  observedProperty: string;
  unit: string;
  status: MonitoringStatus;
  measuringPoint: MonitoringMeasuringPointDto;
  station: MonitoringStationSummaryDto;
  stationOwner: MonitoringStationOwnerDto;
  latestMeasurement: MonitoringLatestMeasurementDto | null;
}
