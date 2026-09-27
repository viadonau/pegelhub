export interface MeasurementWindowDto {
  from: string;
  to: string;
  requested?: string | null;
}

export interface MeasurementIntervalDto {
  from: string;
  to: string;
  mean: number | null;
  observationCount: number;
  supportedNanos: number;
  lastContributingObservedAt: string | null;
  windowStatus: 'closed' | 'open';
  supportStatus: 'full' | 'partial' | 'absent';
}

export interface MeasurementIntervalListDto {
  timeSeriesId: string;
  from: string;
  to: string;
  interval: '15m' | '1h' | '1d';
  timeBasis: 'UTC' | '+01:00';
  closedOnly: boolean;
  representation: string;
  unit: string;
  method: 'time-weighted-step';
  computedAt: string;
  intervals: MeasurementIntervalDto[];
}

export interface MeasurementIntervalRequest {
  from: string;
  to: string;
  interval: '15m' | '1h' | '1d';
  timeBasis: '+01:00' | 'UTC';
}

export interface MeasurementRawRequest {
  from: string;
  to: string;
}

export interface RawMeasurementListDto {
  timeSeriesId: string;
  window: MeasurementWindowDto;
  order: 'asc' | 'desc';
  limit: number;
  truncated: boolean;
  measurements: Array<{ observedAt: string; value: number }>;
  representation: string;
  unit: string;
}
