export interface MonitoringLatestMeasurementDto {
  observedAt: string;
  value: number;
}

export interface MonitoringMeasuringPointSummaryDto {
  id: string;
  name: string;
}

export interface MonitoringMeasuringPointDto {
  id: string;
  name: string;
  referenceLevel: number | null;
  referenceYear: number | null;
  riverKilometer: number | null;
  bank: 'left' | 'right' | null;
  rnw: number | null;
  mw: number | null;
  hsw: number | null;
  hw100: number | null;
}

export interface MonitoringStationSummaryDto {
  id: string;
  stationNumber: string;
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
  externalCode: string | null;
  measuringPoint: MonitoringMeasuringPointDto;
  station: MonitoringStationSummaryDto;
  stationOwner: MonitoringStationOwnerDto;
  latestMeasurement: MonitoringLatestMeasurementDto | null;
}
