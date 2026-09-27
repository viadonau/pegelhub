import { describe, expect, it } from 'vitest';

import {
  createElapsedTimeTicks,
  createYAxisBounds,
  elapsedTimeTickStep,
  formatReferenceLineLabel,
} from './line-chart.presentation';

describe('line-chart presentation', () => {
  it.each([
    ['centimetres', [315, 315], 'cm', 10],
    ['metres', [156.75, 156.75], 'm ü. A.', 0.1],
  ])('gives flat %s data a useful visible span', (_case, values, unit, minimumSpan) => {
    const bounds = createYAxisBounds(values, unit);

    expect(bounds).not.toBeNull();
    expect(bounds!.max - bounds!.min).toBeGreaterThanOrEqual(minimumSpan);
    expect(bounds!.min).toBeLessThanOrEqual(values[0]);
    expect(bounds!.max).toBeGreaterThanOrEqual(values[0]);
  });

  it.each([
    [[1.08, 1.277], 'm³/s'],
    [[-0.01, 0.01], null],
  ])('keeps small variations visible without a one-unit floor', (values, unit) => {
    const bounds = createYAxisBounds(values, unit)!;
    const span = values[1] - values[0];

    expect(bounds.min).toBeLessThanOrEqual(values[0]);
    expect(bounds.max).toBeGreaterThanOrEqual(values[1]);
    expect(bounds.max - bounds.min).toBeLessThanOrEqual(span * 2);
  });

  it('keeps flat zero data on a finite, nonempty axis', () => {
    const bounds = createYAxisBounds([0, 0], 'm³/s')!;

    expect(bounds.min).toBe(0);
    expect(bounds.max).toBeGreaterThan(0);
    expect(Number.isFinite(bounds.max)).toBe(true);
  });

  it('keeps negative data and reference values outside the data range visible', () => {
    const bounds = createYAxisBounds([-8, -4, -20, 12], null);

    expect(bounds?.min).toBeLessThanOrEqual(-20);
    expect(bounds?.max).toBeGreaterThanOrEqual(12);
  });

  it('ignores non-finite input and returns no bounds without finite values', () => {
    expect(createYAxisBounds([Number.NaN, 162, Number.POSITIVE_INFINITY, 480], 'cm')).toMatchObject(
      {
        min: expect.any(Number),
        max: expect.any(Number),
      },
    );
    expect(createYAxisBounds([Number.NaN, Number.NEGATIVE_INFINITY], 'cm')).toBeNull();
  });

  it('formats reference labels with Austrian numbers and an optional unit', () => {
    expect(formatReferenceLineLabel({ label: 'RNW 2020', value: 162.5 }, 'cm')).toBe(
      'RNW 2020 · 162,5 cm',
    );
    expect(formatReferenceLineLabel({ label: 'HSW', value: 480 }, null)).toBe('HSW · 480');
  });

  it.each([
    ['short minutes', '2026-07-22T10:00:00Z', '2026-07-22T10:10:00Z', 5 * 60_000],
    ['short hours', '2026-07-22T10:00:00Z', '2026-07-22T12:00:00Z', 30 * 60_000],
    ['24-hour', '2026-07-22T00:00:00Z', '2026-07-23T00:00:00Z', 6 * 60 * 60_000],
    ['multi-day', '2026-07-16T10:00:00Z', '2026-07-23T10:00:00Z', 24 * 60 * 60_000],
  ])('chooses human-aligned %s time ticks at the fixed MEZ offset', (_, from, to, step) => {
    const min = Date.parse(from);
    const max = Date.parse(to);
    const ticks = createElapsedTimeTicks(min, max);

    expect(elapsedTimeTickStep(min, max)).toBe(step);
    expect(ticks.length).toBeGreaterThan(0);
    expect(ticks.every((tick) => (tick + 60 * 60_000) % step === 0)).toBe(true);
    expect(ticks.every((tick) => tick >= min && tick <= max)).toBe(true);
  });

  it('aligns daily ticks to the selected time basis', () => {
    const from = Date.parse('2026-07-19T10:00:00Z');
    const to = Date.parse('2026-07-26T10:00:00Z');
    expect(createElapsedTimeTicks(from, to, 0)[0]).toBe(Date.parse('2026-07-20T00:00:00Z'));
    expect(createElapsedTimeTicks(from, to, 60 * 60_000)[0]).toBe(
      Date.parse('2026-07-19T23:00:00Z'),
    );
  });
});
