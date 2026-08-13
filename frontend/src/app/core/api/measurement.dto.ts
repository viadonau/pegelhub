export interface MeasurementWindowDto {
  from: string;
  to: string;
  requested?: string | null;
}

export interface MeasurementPointDto {
  observedAt: string;
  value: number;
}

export interface MeasurementListDto {
  timeSeriesId: string;
  window: MeasurementWindowDto | null;
  order: 'asc' | 'desc';
  limit: number;
  truncated: boolean;
  measurements: MeasurementPointDto[];
}

export interface MeasurementResolutionDto {
  bucket: string;
  aggregation: 'average';
  maxPoints: number | null;
}

export interface MeasurementBucketPointDto {
  from: string;
  to: string;
  value: number;
  sampleCount: number;
}

export interface MeasurementBucketListDto {
  timeSeriesId: string;
  window: MeasurementWindowDto | null;
  resolution: MeasurementResolutionDto | null;
  points: MeasurementBucketPointDto[];
}

export const EMPTY_MEASUREMENT_LIST: MeasurementListDto = {
  timeSeriesId: '',
  window: null,
  order: 'desc',
  limit: 0,
  truncated: false,
  measurements: [],
};

export const EMPTY_MEASUREMENT_BUCKET_LIST: MeasurementBucketListDto = {
  timeSeriesId: '',
  window: null,
  resolution: null,
  points: [],
};
