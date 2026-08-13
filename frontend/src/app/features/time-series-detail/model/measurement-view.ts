import { MeasurementBucketListDto, MeasurementListDto } from '../../../core/api/measurement.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
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
  response: MeasurementListDto,
  timeSeries: TimeSeriesDto,
): LatestMeasurementView | null {
  const latest = response.measurements[0];

  if (!latest) {
    return null;
  }

  return {
    timestamp: formatMeasurementTimestamp(latest.observedAt),
    relativeTimestamp: formatRelativeMeasurementAge(latest.observedAt),
    unit: timeSeries.unit || null,
    value: formatMeasurementNumber(latest.value),
  };
}

export function measurementChartSeries(
  response: MeasurementBucketListDto,
  timeSeries: TimeSeriesDto,
): MeasurementChartSeries | null {
  if (response.points.length === 0) {
    return null;
  }

  return {
    name: observedPropertyLabel(timeSeries.observedProperty),
    points: response.points.map((point) => ({
      label: formatTimestamp(point.from, compactTimeFormatter),
      value: point.value,
    })),
  };
}

function formatTimestamp(value: string, formatter: Intl.DateTimeFormat): string {
  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? value : formatter.format(date);
}
