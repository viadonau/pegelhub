export interface ChartAxisBounds {
  min: number;
  max: number;
  precision: number;
  stepSize: number;
}

interface ReferenceLineLabelInput {
  label: string;
  value: number;
}

const Y_AXIS_TARGET_INTERVALS = 4;
const WATER_LEVEL_MIN_VISIBLE_SPAN_CM = 10;
const WATER_LEVEL_MIN_VISIBLE_SPAN_M = 0.1;
const DEFAULT_RELATIVE_MIN_VISIBLE_SPAN = 0.02;
const DEFAULT_MIN_VISIBLE_SPAN = 1;
const FIXED_MEZ_OFFSET_MS = 60 * 60 * 1_000;
const TIME_TICK_TARGET_COUNT = 8;
const ELAPSED_TIME_STEPS_MS = [
  60_000,
  5 * 60_000,
  15 * 60_000,
  30 * 60_000,
  60 * 60_000,
  3 * 60 * 60_000,
  6 * 60 * 60_000,
  12 * 60 * 60_000,
  24 * 60 * 60_000,
  2 * 24 * 60 * 60_000,
  7 * 24 * 60 * 60_000,
  14 * 24 * 60 * 60_000,
  30 * 24 * 60 * 60_000,
];

export function createYAxisBounds(
  values: readonly number[],
  unit: string | null | undefined,
): ChartAxisBounds | null {
  const finiteValues = values.filter(Number.isFinite);

  if (finiteValues.length === 0) {
    return null;
  }

  const dataMin = Math.min(...finiteValues);
  const dataMax = Math.max(...finiteValues);
  const dataSpan = dataMax - dataMin;
  const center = (dataMin + dataMax) / 2;
  const minimumVisibleSpan = getMinimumVisibleSpan(unit, center);
  // A one-unit fallback is only needed for flat zero data; it flattens fractional signals otherwise.
  const targetSpan = Math.max(dataSpan * 1.3, minimumVisibleSpan) || DEFAULT_MIN_VISIBLE_SPAN;
  const stepSize = niceNumber(targetSpan / Y_AXIS_TARGET_INTERVALS);
  const halfSpan = targetSpan / 2;
  let min = Math.floor((center - halfSpan) / stepSize) * stepSize;
  let max = Math.ceil((center + halfSpan) / stepSize) * stepSize;

  if (dataMin >= 0 && min < 0) {
    min = 0;
    max = Math.ceil(Math.max(dataMax, min + targetSpan) / stepSize) * stepSize;
  }

  if (min === max) {
    max = min + stepSize * Y_AXIS_TARGET_INTERVALS;
  }

  const precision = precisionForStep(stepSize);

  return {
    min: normalizeAxisNumber(min, precision),
    max: normalizeAxisNumber(max, precision),
    precision,
    stepSize: normalizeAxisNumber(stepSize, precision),
  };
}

export function formatReferenceLineLabel(
  line: ReferenceLineLabelInput,
  unit: string | null | undefined,
): string {
  const suffix = unit ? ` ${unit}` : '';

  return `${line.label} · ${formatChartNumber(line.value)}${suffix}`;
}

export function formatChartNumber(value: number, maximumFractionDigits = 3): string {
  return new Intl.NumberFormat('de-AT', {
    maximumFractionDigits,
  }).format(value);
}

export function elapsedTimeTickStep(from: number, to: number): number | null {
  const duration = to - from;
  if (!Number.isFinite(duration) || duration <= 0) {
    return null;
  }

  return (
    ELAPSED_TIME_STEPS_MS.find(
      (step) => Math.ceil(duration / step) <= TIME_TICK_TARGET_COUNT - 1,
    ) ?? ELAPSED_TIME_STEPS_MS.at(-1)!
  );
}

export function createElapsedTimeTicks(
  from: number,
  to: number,
  offsetMs = FIXED_MEZ_OFFSET_MS,
): number[] {
  const step = elapsedTimeTickStep(from, to);
  if (step === null) {
    return [];
  }

  const first = Math.ceil((from + offsetMs) / step) * step - offsetMs;
  const ticks: number[] = [];

  for (let tick = first; tick <= to; tick += step) {
    if (tick >= from) {
      ticks.push(tick);
    }
  }

  return ticks.length > 0 ? ticks : [from, to];
}

function getMinimumVisibleSpan(unit: string | null | undefined, center: number): number {
  const normalizedUnit = unit?.trim().toLowerCase().replace(/\s+/g, '') ?? '';

  if (normalizedUnit === 'cm') {
    return WATER_LEVEL_MIN_VISIBLE_SPAN_CM;
  }

  if (
    normalizedUnit === 'm' ||
    normalizedUnit === 'meter' ||
    normalizedUnit === 'metre' ||
    normalizedUnit.startsWith('mü') ||
    normalizedUnit.startsWith('mue')
  ) {
    return WATER_LEVEL_MIN_VISIBLE_SPAN_M;
  }

  return Math.abs(center) * DEFAULT_RELATIVE_MIN_VISIBLE_SPAN;
}

function niceNumber(value: number): number {
  if (!Number.isFinite(value) || value <= 0) {
    return DEFAULT_MIN_VISIBLE_SPAN;
  }

  const exponent = Math.floor(Math.log10(value));
  const fraction = value / 10 ** exponent;
  const niceFraction = fraction < 1.5 ? 1 : fraction < 3 ? 2 : fraction < 7 ? 5 : 10;

  return niceFraction * 10 ** exponent;
}

function precisionForStep(step: number): number {
  if (!Number.isFinite(step) || step <= 0) {
    return 0;
  }

  return Math.max(0, Math.ceil(-Math.log10(step)));
}

function normalizeAxisNumber(value: number, precision: number): number {
  return Number(value.toFixed(precision + 2));
}
