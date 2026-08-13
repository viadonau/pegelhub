export interface TimeSeriesDto {
  id: string;
  measuringPointId: string;
  observedProperty: string;
  unit: string;
  externalCode?: string | null;
  sourceConnectorId?: string | null;
}
