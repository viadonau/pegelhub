export interface MeasuringPointDto {
  id: string;
  stationId: string;
  name: string;
  referenceLevel?: number | null;
  referenceYear?: number | null;
  riverKilometer?: number | null;
  bank?: string | null;
  rnw?: number | null;
  mw?: number | null;
  hsw?: number | null;
  hw100?: number | null;
}
