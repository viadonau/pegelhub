import {
  MeasurementIntervalListDto,
  MeasurementIntervalRequest,
  MeasurementRawRequest,
} from '../../../core/api/measurement.dto';
import { MeasurementChartSeries } from './measurement-view';

export type AnalysisInterval = MeasurementIntervalRequest['interval'];
export type AnalysisTimeBasis = MeasurementIntervalRequest['timeBasis'];
export type AnalysisRange = '24h' | '7d' | '30d' | 'custom';
export const MAX_ANALYSIS_INTERVALS = 50_000;

export interface LoadedAnalysisResult {
  query: MeasurementIntervalRequest;
  response: MeasurementIntervalListDto;
}

export type RequestedAnalysisRangeResult =
  | { range: MeasurementRawRequest; error: null }
  | { range: null; error: string };

export type AnalysisQueryResult =
  | { query: MeasurementIntervalRequest; error: null }
  | { query: null; error: string };

const WIDTH: Record<AnalysisInterval, number> = {
  '15m': 15 * 60_000,
  '1h': 60 * 60_000,
  '1d': 24 * 60 * 60_000,
};
const DAY = 24 * 60 * 60_000;

export function buildAnalysisQuery(
  range: AnalysisRange,
  interval: AnalysisInterval,
  timeBasis: AnalysisTimeBasis,
  customFrom: string,
  customTo: string,
  now = Date.now(),
): AnalysisQueryResult {
  const requested = resolveRequestedRange(range, timeBasis, customFrom, customTo, now);
  if (!requested.range) return { query: null, error: requested.error };
  const offset = timeBasis === '+01:00' ? 60 * 60_000 : 0;
  const width = WIDTH[interval];
  const requestedFrom = Date.parse(requested.range.from);
  const requestedTo = Date.parse(requested.range.to);

  const from = Math.floor((requestedFrom + offset) / width) * width - offset;
  const to = Math.ceil((requestedTo + offset) / width) * width - offset;
  const windows = (to - from) / width;
  if (windows > MAX_ANALYSIS_INTERVALS) {
    return {
      query: null,
      error: `Der Zeitraum umfasst ${windows.toLocaleString('de-DE')} Intervalle (maximal ${MAX_ANALYSIS_INTERVALS.toLocaleString('de-DE')}). Zeitraum verkürzen oder ein größeres Mittelungsintervall wählen.`,
    };
  }

  return {
    query: {
      interval,
      timeBasis,
      from: new Date(from).toISOString(),
      to: new Date(to).toISOString(),
    },
    error: null,
  };
}

export function resolveRequestedRange(
  range: AnalysisRange,
  timeBasis: AnalysisTimeBasis,
  customFrom: string,
  customTo: string,
  now = Date.now(),
): RequestedAnalysisRangeResult {
  let requestedFrom: number;
  let requestedTo: number;

  if (range === 'custom') {
    requestedFrom = parseBasisDateTime(customFrom, timeBasis);
    requestedTo = parseBasisDateTime(customTo, timeBasis);
    if (!Number.isFinite(requestedFrom) || !Number.isFinite(requestedTo)) {
      return { range: null, error: 'Bitte gültige Start- und Endzeiten eingeben.' };
    }
  } else {
    requestedTo = now;
    requestedFrom = requestedTo - (range === '24h' ? DAY : range === '7d' ? 7 * DAY : 30 * DAY);
  }

  if (
    !Number.isFinite(requestedFrom) ||
    !Number.isFinite(requestedTo) ||
    requestedFrom >= requestedTo
  ) {
    return { range: null, error: 'Der Beginn muss vor dem Ende liegen.' };
  }
  return {
    range: { from: new Date(requestedFrom).toISOString(), to: new Date(requestedTo).toISOString() },
    error: null,
  };
}

export function formatBasisDateTime(
  value: string,
  timeBasis: AnalysisTimeBasis,
  seconds = false,
  milliseconds = false,
): string {
  return new Intl.DateTimeFormat('de-AT', {
    timeZone: timeBasis === '+01:00' ? 'Etc/GMT-1' : 'UTC',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...(seconds ? { second: '2-digit' } : {}),
    ...(milliseconds ? { fractionalSecondDigits: 3 as const } : {}),
    hourCycle: 'h23',
  }).format(new Date(value));
}

export function formatRawNumber(value: number): string {
  return (Object.is(value, -0) ? '-0' : String(value)).replace('.', ',');
}

export function basisDateTimeInput(value: number, timeBasis: AnalysisTimeBasis): string {
  const offset = timeBasis === '+01:00' ? 60 * 60_000 : 0;
  return new Date(value + offset).toISOString().slice(0, 16);
}

function parseBasisDateTime(value: string, timeBasis: AnalysisTimeBasis): number {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) return NaN;
  const parsed = Date.parse(`${value}:00Z`);
  if (!Number.isFinite(parsed) || new Date(parsed).toISOString().slice(0, 16) !== value) return NaN;
  return parsed - (timeBasis === '+01:00' ? 60 * 60_000 : 0);
}

export function analysisChartSeries(
  result: LoadedAnalysisResult,
  name: string,
): MeasurementChartSeries {
  return {
    name,
    intervalLabel: { '15m': '15 Min.', '1h': '1 Std.', '1d': '1 Tag' }[result.query.interval],
    window: {
      from: Date.parse(result.query.from),
      to: Date.parse(result.response.intervals.at(-1)?.to ?? result.query.from),
    },
    points: result.response.intervals.flatMap((item) => {
      if (item.mean === null) return [];
      const from = Date.parse(item.from);
      const to = Date.parse(item.to);
      return [
        {
          from,
          to,
          x: to,
          value: item.mean,
          observationCount: item.observationCount,
          lastContributingObservedAt: item.lastContributingObservedAt,
        },
      ];
    }),
  };
}
