import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { RUNTIME_CONFIG } from '../../../core/config/runtime-config';
import { MeasurementIntervalListDto } from '../../../core/api/measurement.dto';
import { TEST_RUNTIME_CONFIG } from '../../../../testing/fixtures';
import { PhMeasurementChartComponent } from '../measurement-chart/measurement-chart.component';
import { PhTimeSeriesMeasurementsComponent } from './time-series-measurements.component';

const path = '/api/v1/time-series/series-1/measurements';

describe('PhTimeSeriesMeasurementsComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PhTimeSeriesMeasurementsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RUNTIME_CONFIG, useValue: TEST_RUNTIME_CONFIG },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('keeps the complete interval response behind the chart, including zero and empty evidence', async () => {
    const fixture = create();
    const request = intervalRequest();
    request.flush(
      response(request, [
        interval('2026-07-19T10:00:00Z', 0, 1),
        interval('2026-07-19T10:15:00Z', null, 0),
        interval('2026-07-19T10:30:00Z', 3, 2),
      ]),
    );
    await settled(fixture);

    expect(text(fixture)).toContain('2 Mittelwerte');
    expect(text(fixture)).toContain('1 Intervall ohne vollständige Stützung');
    expect(text(fixture)).not.toContain('Zeitgewichtetes Mittel der gespeicherten Messwerte');
    http.expectNone((req) => req.url === `${path}/intervals`);
  });

  it('retains all-empty window evidence in chart mode', async () => {
    const fixture = create();
    const request = intervalRequest();
    request.flush(
      response(request, [
        interval('2026-07-19T10:00:00Z', null, 0),
        interval('2026-07-19T10:15:00Z', null, 0),
      ]),
    );
    await settled(fixture);
    expect(text(fixture)).toContain('2 Intervalle ohne vollständige Stützung');
    expect(text(fixture)).toContain('2 abgeschlossene Intervalle ohne vollständige Stützung.');
  });

  it('shows a carried temperature mean with no new measurements and its source age', async () => {
    const fixture = create();
    const request = intervalRequest();
    request.flush({
      ...response(request, [
        {
          ...interval('2026-07-19T10:00:00Z', 18, 0),
          lastContributingObservedAt: '2026-07-18T08:00:12.123Z',
        },
      ]),
    });
    await settled(fixture);
    const chart = fixture.debugElement.query(By.directive(PhMeasurementChartComponent))
      .componentInstance as unknown as {
      lineChartSeries(): { points: Array<{ value: number; tooltipFooter: string }> };
    };
    expect(chart.lineChartSeries().points[0]).toMatchObject({
      value: 18,
      tooltipFooter: 'Fortgeführt · letzter Messwert 26 h 14 min alt',
    });
  });

  it('displays the known unit symbol without changing the canonical result or raw payload', async () => {
    const fixture = create();
    fixture.componentRef.setInput('observedProperty', 'water-temperature');
    fixture.componentRef.setInput('unit', '°C');
    fixture.detectChanges();
    const request = intervalRequest();
    request.flush({ ...response(request, [interval('2026-07-19T10:00:00Z', 12, 1)]), unit: 'Cel' });
    await settled(fixture);
    expect(text(fixture)).toContain('Wassertemperatur (°C)');
    clickView(fixture, 'Rohwerte');
    rawRequest().flush({
      ...raw(false, [{ observedAt: '2026-07-19T10:00:10Z', value: 12 }]),
      unit: 'Cel',
    });
    await settled(fixture);
    expect(text(fixture)).toContain('12 °C');
    expect(text(fixture)).not.toContain('12 Cel');
  });

  it('preserves the selected interval across range changes and handles a rejected cap', async () => {
    const fixture = create();
    intervalRequest().flush(response(null, []));
    await settled(fixture);
    controls(fixture).setRange('7d');
    fixture.detectChanges();
    TestBed.tick();
    expect(intervalRequest().request.params.get('interval')).toBe('15m');
    controls(fixture).setRange('30d');
    fixture.detectChanges();
    TestBed.tick();
    const month = intervalRequest();
    expect(month.request.params.get('interval')).toBe('15m');
    month.flush(response(month, []));
    await settled(fixture);

    controls(fixture).setRange('custom');
    controls(fixture).setCustomFrom('2025-01-01T00:00');
    controls(fixture).setCustomTo('2026-07-01T00:00');
    fixture.detectChanges();
    TestBed.tick();
    expect(text(fixture)).toContain('maximal 50.000');
    http.expectNone((req) => req.url === `${path}/intervals`);
    controls(fixture).setInterval('1h');
    fixture.detectChanges();
    TestBed.tick();
    expect(intervalRequest().request.params.get('interval')).toBe('1h');
  });

  it('keeps bounded raw inspection independent of the selected mean interval', async () => {
    const fixture = create();
    intervalRequest().flush(response(null, []));
    await settled(fixture);
    controls(fixture).setRange('30d');
    fixture.detectChanges();
    TestBed.tick();
    intervalRequest().flush(response(null, []));
    await settled(fixture);
    clickView(fixture, 'Rohwerte');
    const first = rawRequest();
    const requested = [first.request.params.get('from'), first.request.params.get('to')];
    expect(Date.parse(requested[1]!) - Date.parse(requested[0]!)).toBe(30 * 24 * 60 * 60_000);
    first.flush(raw(false, [{ observedAt: '2026-07-19T10:00:10.123Z', value: 1.0001 }]));
    await settled(fixture);
    expect(text(fixture)).toContain('1,0001 cm');

    controls(fixture).setInterval('1d');
    fixture.detectChanges();
    TestBed.tick();
    intervalRequest().flush(response(null, []));
    http.expectNone((req) => req.url === path);
  });

  it('refuses a raw read wider than 30 days without silently truncating the range', async () => {
    const fixture = create();
    const initial = intervalRequest();
    controls(fixture).setRange('custom');
    controls(fixture).setInterval('1d');
    controls(fixture).setCustomFrom('2026-01-01T00:00');
    controls(fixture).setCustomTo('2026-02-02T00:00');
    fixture.detectChanges();
    TestBed.tick();
    expect(initial.cancelled).toBe(true);
    intervalRequest().flush(response(null, []));
    await settled(fixture);
    clickView(fixture, 'Rohwerte');
    expect(text(fixture)).toContain('höchstens 30 Tage');
    http.expectNone((req) => req.url === path);
  });

  it('shows full-window widening while raw inspection keeps the requested custom bounds', async () => {
    const fixture = create();
    const initial = intervalRequest();
    controls(fixture).setRange('custom');
    controls(fixture).setCustomFrom('2026-07-19T10:20');
    controls(fixture).setCustomTo('2026-07-19T11:10');
    fixture.detectChanges();
    TestBed.tick();
    expect(initial.cancelled).toBe(true);
    const request = intervalRequest();
    expect(request.request.params.get('from')).toBe('2026-07-19T09:15:00.000Z');
    expect(request.request.params.get('to')).toBe('2026-07-19T10:15:00.000Z');
    expect(text(fixture)).not.toContain('Abfrage voller Intervalle');
    request.flush(response(request, []));
    await settled(fixture);
    clickView(fixture, 'Rohwerte');
    const rawRead = rawRequest();
    expect(rawRead.request.params.get('from')).toBe('2026-07-19T09:20:00.000Z');
    expect(rawRead.request.params.get('to')).toBe('2026-07-19T10:10:00.000Z');
    rawRead.flush(raw(false, []));
    await settled(fixture);
    controls(fixture).setCustomFrom('2026-02-30T10:00');
    fixture.detectChanges();
    TestBed.tick();
    expect(text(fixture)).toContain('gültige Start- und Endzeiten');
    http.expectNone((req) => req.url === `${path}/intervals` || req.url === path);
  });

  it('preserves a custom time range and cancels the old request when changing time basis', async () => {
    const fixture = create();
    const initial = intervalRequest();
    controls(fixture).setRange('custom');
    controls(fixture).setCustomFrom('2026-07-19T10:20');
    controls(fixture).setCustomTo('2026-07-19T11:10');
    fixture.detectChanges();
    TestBed.tick();
    expect(initial.cancelled).toBe(true);
    const old = intervalRequest();
    controls(fixture).setTimeBasis('UTC');
    fixture.detectChanges();
    TestBed.tick();
    const next = intervalRequest();
    expect(next.request.params.get('timeBasis')).toBe('UTC');
    expect(next.request.params.get('from')).toBe(old.request.params.get('from'));
    expect(next.request.params.get('to')).toBe(old.request.params.get('to'));
    expect(old.cancelled).toBe(true);
    next.flush(response(next, [interval('2026-07-19T10:00:00Z', 7, 1)]));
    await settled(fixture);
    expect(text(fixture)).toContain('UTC');
  });

  it('shows truncated raw data as a refine-range state, then pages only complete raw results', async () => {
    const fixture = create();
    intervalRequest().flush(response(null, []));
    await settled(fixture);
    clickView(fixture, 'Rohwerte');
    const first = rawRequest();
    expect(first.request.params.get('limit')).toBe('1000');
    first.flush(raw(true, [{ observedAt: '2026-07-19T10:00:00Z', value: 1 }]));
    await settled(fixture);
    expect(text(fixture)).toContain('bitte den Zeitraum verkürzen');
    expect(fixture.nativeElement.querySelector('table')).toBeNull();

    controls(fixture).setRange('7d');
    fixture.detectChanges();
    TestBed.tick();
    intervalRequest().flush(response(null, []));
    const second = rawRequest();
    const values = Array.from({ length: 51 }, (_, index) => ({
      observedAt:
        index === 0
          ? '2026-07-19T10:00:10.123Z'
          : index === 1
            ? '2026-07-19T10:00:45.456Z'
            : '2026-07-19T10:01:00Z',
      value: index === 0 ? 1.0001 : index === 1 ? 1.0002 : index,
    }));
    second.flush(raw(false, values));
    await settled(fixture);
    expect(text(fixture)).toContain('Seite 1 von 2');
    expect(text(fixture)).toContain('11:00:10');
    expect(text(fixture)).toContain('11:00:45');
    expect(text(fixture)).toContain('123');
    expect(text(fixture)).toContain('456');
    expect(text(fixture)).toContain('1,0001');
    expect(text(fixture)).toContain('1,0002');
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(50);
    (
      fixture.nativeElement.querySelector('[aria-label="Nächste Seite"]') as HTMLButtonElement
    ).click();
    fixture.detectChanges();
    expect(text(fixture)).toContain('Seite 2 von 2');
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);

    fixture.componentRef.setInput('timeSeriesId', 'series-2');
    fixture.detectChanges();
    TestBed.tick();
    http
      .expectOne((req) => req.url === '/api/v1/time-series/series-2/measurements/intervals')
      .flush(response(null, []));
    http
      .expectOne((req) => req.url === '/api/v1/time-series/series-2/measurements')
      .flush(raw(false, [values[0]]));
    await settled(fixture);
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(text(fixture)).not.toContain('Seite 2');
    expect(text(fixture)).toContain('11:00:10');
  });

  function intervalRequest(): TestRequest {
    return http.expectOne((req) => req.url === `${path}/intervals`);
  }
  function rawRequest(): TestRequest {
    return http.expectOne((req) => req.url === path);
  }
});

function create(): ComponentFixture<PhTimeSeriesMeasurementsComponent> {
  const fixture = TestBed.createComponent(PhTimeSeriesMeasurementsComponent);
  fixture.componentRef.setInput('timeSeriesId', 'series-1');
  fixture.componentRef.setInput('observedProperty', 'water-level');
  fixture.componentRef.setInput('unit', 'cm');
  fixture.detectChanges();
  TestBed.tick();
  return fixture;
}

async function settled(fixture: ComponentFixture<unknown>): Promise<void> {
  await vi.waitFor(() => {
    TestBed.tick();
    fixture.detectChanges();
    const state = fixture.componentInstance as unknown as {
      loading(): boolean;
      rawLoading(): boolean;
    };
    expect(state.loading()).toBe(false);
    expect(state.rawLoading()).toBe(false);
  });
}

function controls(fixture: ComponentFixture<PhTimeSeriesMeasurementsComponent>) {
  return fixture.componentInstance as unknown as {
    setRange(range: '7d' | '30d' | 'custom'): void;
    setInterval(interval: '1h' | '1d'): void;
    setTimeBasis(basis: 'UTC'): void;
    setCustomFrom(value: string): void;
    setCustomTo(value: string): void;
  };
}

function clickView(fixture: ComponentFixture<unknown>, label: string): void {
  const button = [...fixture.nativeElement.querySelectorAll('.ph-analysis-view button')].find(
    (node: Element) => node.textContent?.includes(label),
  ) as HTMLButtonElement;
  button.click();
  fixture.detectChanges();
  TestBed.tick();
}

function response(
  request: TestRequest | null,
  intervals: MeasurementIntervalListDto['intervals'],
): MeasurementIntervalListDto {
  return {
    timeSeriesId: 'series-1',
    from: request?.request.params.get('from') ?? '2026-07-19T00:00:00Z',
    to: request?.request.params.get('to') ?? '2026-07-20T00:00:00Z',
    interval: '15m',
    timeBasis: request?.request.params.get('timeBasis') === 'UTC' ? 'UTC' : '+01:00',
    closedOnly: true,
    representation: 'canonical',
    unit: 'cm',
    method: 'time-weighted-step',
    computedAt: '2026-07-20T12:00:00Z',
    intervals,
  };
}

function interval(
  from: string,
  mean: number | null,
  observationCount: number,
): MeasurementIntervalListDto['intervals'][number] {
  return {
    from,
    to: new Date(Date.parse(from) + 15 * 60_000).toISOString(),
    mean,
    observationCount,
    supportedNanos: mean === null ? 0 : 15 * 60 * 1_000_000_000,
    lastContributingObservedAt: mean === null ? null : from,
    windowStatus: 'closed',
    supportStatus: mean === null ? 'absent' : 'full',
  };
}

function raw(truncated: boolean, measurements: Array<{ observedAt: string; value: number }>) {
  return {
    timeSeriesId: 'series-1',
    window: { from: '2026-07-19T00:00:00Z', to: '2026-07-20T00:00:00Z', requested: null },
    order: 'asc',
    limit: 1000,
    truncated,
    measurements,
    representation: 'canonical',
    unit: 'cm',
  };
}

function text(fixture: ComponentFixture<unknown>): string {
  return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
}
