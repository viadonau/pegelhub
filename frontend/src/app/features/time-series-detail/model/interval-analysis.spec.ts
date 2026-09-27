import { describe, expect, it } from 'vitest';

import { MeasurementIntervalListDto } from '../../../core/api/measurement.dto';
import {
  analysisChartSeries,
  buildAnalysisQuery,
  formatBasisDateTime,
  resolveRequestedRange,
} from './interval-analysis';

describe('interval analysis query', () => {
  it('aligns a fixed MEZ calendar day at local midnight, even in summer', () => {
    const result = buildAnalysisQuery(
      'custom',
      '1d',
      '+01:00',
      '2026-07-19T00:00',
      '2026-07-20T00:00',
    );
    expect(result.query).toMatchObject({
      from: '2026-07-18T23:00:00.000Z',
      to: '2026-07-19T23:00:00.000Z',
    });
    expect(formatBasisDateTime('2026-07-19T12:00:00Z', '+01:00')).toContain('13:00');
    expect(formatBasisDateTime('2026-07-19T12:00:00Z', 'UTC')).toContain('12:00');
    expect(formatBasisDateTime('2026-07-19T11:31:10.123Z', 'UTC', true, true)).toContain('123');
  });

  it('widens custom bounds to full intervals', () => {
    expect(
      buildAnalysisQuery('custom', '1h', 'UTC', '2026-07-19T10:20', '2026-07-19T11:10').query,
    ).toMatchObject({
      from: '2026-07-19T10:00:00.000Z',
      to: '2026-07-19T12:00:00.000Z',
    });
  });

  it('does not change interval to accommodate a long range', () => {
    const now = Date.parse('2026-07-20T12:35:00Z');
    expect(buildAnalysisQuery('24h', '15m', '+01:00', '', '', now).query?.interval).toBe('15m');
    expect(buildAnalysisQuery('7d', '15m', '+01:00', '', '', now).query?.interval).toBe('15m');
    expect(buildAnalysisQuery('30d', '15m', '+01:00', '', '', now)).toMatchObject({
      query: {
        interval: '15m',
        from: '2026-06-20T12:30:00.000Z',
        to: '2026-07-20T12:45:00.000Z',
      },
      error: null,
    });
    expect(buildAnalysisQuery('30d', '1h', '+01:00', '', '', now).query?.interval).toBe('1h');
    expect(resolveRequestedRange('30d', '+01:00', '', '', now).range).not.toBeNull();
    expect(
      resolveRequestedRange('24h', '+01:00', '', '', Date.parse('2026-07-20T12:37:00Z')).range?.to,
    ).toBe('2026-07-20T12:37:00.000Z');
  });

  it.each(['2026-02-30T10:00', '2026-13-01T10:00', '2026-01-01T25:00', 'bad', ''])(
    'rejects invalid local time %s',
    (value) => {
      expect(
        buildAnalysisQuery('custom', '15m', '+01:00', value, '2026-03-01T12:00').query,
      ).toBeNull();
    },
  );

  it('rejects reversed bounds and counts outward rounding against the 50,000-window limit', () => {
    expect(
      buildAnalysisQuery('custom', '1h', 'UTC', '2026-07-20T12:00', '2026-07-19T12:00').query,
    ).toBeNull();
    expect(
      buildAnalysisQuery('custom', '15m', 'UTC', '2025-01-01T00:00', '2026-06-05T20:00').query,
    ).not.toBeNull();
    expect(
      buildAnalysisQuery('custom', '15m', 'UTC', '2025-01-01T00:00', '2026-06-05T20:01'),
    ).toMatchObject({ query: null, error: expect.stringContaining('maximal 50.000') });
  });
});

describe('chart projection', () => {
  it('keeps zero, sparse windows and gaps without reducing the shared result', () => {
    const response: MeasurementIntervalListDto = {
      timeSeriesId: 'series-1',
      from: '2026-07-19T00:00:00Z',
      to: '2026-07-19T03:00:00Z',
      interval: '1h',
      timeBasis: 'UTC',
      closedOnly: true,
      representation: 'canonical',
      unit: 'cm',
      method: 'time-weighted-step',
      computedAt: '2026-07-19T04:00:00Z',
      intervals: [
        {
          from: '2026-07-19T00:00:00Z',
          to: '2026-07-19T01:00:00Z',
          mean: 0,
          observationCount: 1,
          supportedNanos: 3_600_000_000_000,
          lastContributingObservedAt: '2026-07-19T00:05:00Z',
          windowStatus: 'closed',
          supportStatus: 'full',
        },
        {
          from: '2026-07-19T01:00:00Z',
          to: '2026-07-19T02:00:00Z',
          mean: null,
          observationCount: 0,
          supportedNanos: 0,
          lastContributingObservedAt: null,
          windowStatus: 'closed',
          supportStatus: 'absent',
        },
        {
          from: '2026-07-19T02:00:00Z',
          to: '2026-07-19T03:00:00Z',
          mean: 3,
          observationCount: 2,
          supportedNanos: 3_600_000_000_000,
          lastContributingObservedAt: '2026-07-19T02:15:00Z',
          windowStatus: 'closed',
          supportStatus: 'full',
        },
      ],
    };
    const query = buildAnalysisQuery(
      'custom',
      '1h',
      'UTC',
      '2026-07-19T00:00',
      '2026-07-19T03:00',
    ).query!;
    const series = analysisChartSeries({ query, response }, 'Wasserstand');
    expect(series.points.map((point) => point.value)).toEqual([0, 3]);
    expect(series.points.map((point) => point.observationCount)).toEqual([1, 2]);
    expect(series.points.map((point) => point.x)).toEqual([
      Date.parse('2026-07-19T01:00:00Z'),
      Date.parse('2026-07-19T03:00:00Z'),
    ]);
    expect(
      analysisChartSeries(
        { query, response: { ...response, intervals: response.intervals.slice(0, 1) } },
        'Wasserstand',
      ).window?.to,
    ).toBe(Date.parse('2026-07-19T01:00:00Z'));
  });
});
