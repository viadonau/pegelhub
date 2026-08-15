import { MonitoringLatestMeasurementDto } from '../../../core/api/monitoring.dto';
import { MeasurementBucketListDto } from '../../../core/api/measurement.dto';
import {
  formatMeasurementNumber,
  formatMeasurementTimestamp,
  formatRelativeMeasurementAge,
  UI_LOCALE,
} from '../../../core/measurement/measurement-format';
import { observedPropertyLabel } from '../../../core/time-series/parameter-legend';

export interface MeasurementChartSeries {
  name: string;
  points: Array<{ label: string; value: number }>;
}

export interface LatestMeasurementView {
  timestamp: string;
  relativeTimestamp: string | null;
  unit: string | null;
  value: string;
}

const compactTimeFormatter = new Intl.DateTimeFormat(UI_LOCALE, {
  month: 'short',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

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

export function measurementChartSeries(
  response: MeasurementBucketListDto,
  observedProperty: string,
): MeasurementChartSeries | null {
  if (response.points.length === 0) {
    return null;
  }

  return {
    name: observedPropertyLabel(observedProperty),
    points: response.points.map((point) => ({
      label: formatTimestamp(point.from),
      value: point.value,
    })),
  };
}

function formatTimestamp(value: string): string {
  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? value : compactTimeFormatter.format(date);
}
