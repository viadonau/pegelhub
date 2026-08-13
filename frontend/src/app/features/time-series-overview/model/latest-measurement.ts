import { MeasurementPointDto } from '../../../core/api/measurement.dto';

export type LatestMeasurementResult =
  | { status: 'loading' }
  | { status: 'available'; measurement: MeasurementPointDto }
  | { status: 'empty' }
  | { status: 'error' };

export type LatestMeasurementLookup = ReadonlyMap<string, LatestMeasurementResult>;
