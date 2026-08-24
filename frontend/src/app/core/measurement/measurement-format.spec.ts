import { describe, expect, it } from 'vitest';

import { formatMeasurementValue, formatRelativeMeasurementAge } from './measurement-format';

describe('measurement formatting', () => {
  it('formats values with the Austrian locale and unit', () => {
    expect(formatMeasurementValue(1940.1254, 'm³/s')).toBe('1\u00a0940,125 m³/s');
  });

  it('describes relative age at each operator-facing boundary', () => {
    const now = new Date('2026-07-20T12:00:00Z');

    expect(formatRelativeMeasurementAge('2026-07-20T12:00:00Z', now)).toBe('gerade eben');
    expect(formatRelativeMeasurementAge('2026-07-20T11:59:00Z', now)).toBe('vor 1 Min.');
    expect(formatRelativeMeasurementAge('2026-07-20T11:01:00Z', now)).toBe('vor 59 Min.');
    expect(formatRelativeMeasurementAge('2026-07-20T11:00:00Z', now)).toBe('vor 1 Std.');
    expect(formatRelativeMeasurementAge('2026-07-19T13:00:00Z', now)).toBe('vor 23 Std.');
    expect(formatRelativeMeasurementAge('2026-07-19T12:00:00Z', now)).toBe('vor 1 Tag');
    expect(formatRelativeMeasurementAge('2026-07-14T12:00:00Z', now)).toBe('vor 6 Tagen');
    expect(formatRelativeMeasurementAge('2026-07-13T12:00:00Z', now)).toBeNull();
    expect(formatRelativeMeasurementAge('2026-07-20T12:01:00Z', now)).toBeNull();
    expect(formatRelativeMeasurementAge('not-a-timestamp', now)).toBeNull();

    expect(formatRelativeMeasurementAge('2026-07-20T11:52:00Z', now)).toBe('vor 8 Min.');
  });
});
