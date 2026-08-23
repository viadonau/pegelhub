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
  const targetSpan = Math.max(dataSpan * 1.3, minimumVisibleSpan);
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

export function formatChartNumber(value: number): string {
  return new Intl.NumberFormat('de-AT', {
    maximumFractionDigits: 3,
  }).format(value);
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

  const relativeSpan = niceNumber(Math.abs(center) * DEFAULT_RELATIVE_MIN_VISIBLE_SPAN);

  return Math.max(relativeSpan, DEFAULT_MIN_VISIBLE_SPAN);
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
