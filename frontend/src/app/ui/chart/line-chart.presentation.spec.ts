import { describe, expect, it } from 'vitest';

import { createYAxisBounds, formatReferenceLineLabel } from './line-chart.presentation';

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
});
