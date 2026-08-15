export interface MeasurementWindowDto {
  from: string;
  to: string;
  requested?: string | null;
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
