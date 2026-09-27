import { MonitoringLatestMeasurementDto } from '../../../core/api/monitoring.dto';
import {
  formatMeasurementNumber,
  formatMeasurementTimestamp,
  formatRelativeMeasurementAge,
} from '../../../core/measurement/measurement-format';

export interface MeasurementChartSeries {
  name: string;
  intervalLabel: string | null;
  window: { from: number; to: number } | null;
  points: Array<{
    from: number;
    to: number;
    x: number;
    value: number;
    observationCount: number;
    lastContributingObservedAt: string | null;
  }>;
}

export interface LatestMeasurementView {
  timestamp: string;
  relativeTimestamp: string | null;
  unit: string | null;
  value: string;
}

export function latestMeasurementView(
  response: MonitoringLatestMeasurementDto | null,
  unit: string,
): LatestMeasurementView | null {
  if (!response) {
    return null;
  }

  return {
    timestamp: formatMeasurementTimestamp(response.observedAt),
    relativeTimestamp: formatRelativeMeasurementAge(response.observedAt),
    unit: unit || null,
    value: formatMeasurementNumber(response.value),
  };
}
