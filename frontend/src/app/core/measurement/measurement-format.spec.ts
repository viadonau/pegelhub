import { describe, expect, it } from 'vitest';

import { formatMeasurementValue, formatRelativeMeasurementAge } from './measurement-format';

describe('measurement formatting', () => {
  it('formats values with the Austrian locale and unit', () => {
    expect(formatMeasurementValue(1940.1254, 'm³/s')).toBe('1\u00a0940,125 m³/s');
  });

  it('describes recent measurement age without imposing a freshness threshold', () => {
    const now = new Date('2026-07-20T12:00:00Z');

    expect(formatRelativeMeasurementAge('2026-07-20T11:52:00Z', now)).toBe('vor 8 Min.');
    expect(formatRelativeMeasurementAge('2026-07-19T11:52:00Z', now)).toBe('vor 1 Tag');
    expect(formatRelativeMeasurementAge('2026-07-01T11:52:00Z', now)).toBeNull();
  });
});
