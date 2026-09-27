import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { Chart } from 'chart.js';
import { providePrimeNG } from 'primeng/config';
import { page, userEvent } from 'vitest/browser';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { App } from './app';
import { routes } from './app.routes';
import { AuthStateService } from './core/auth/auth-state.service';
import { RUNTIME_CONFIG } from './core/config/runtime-config';
import { ThemeService } from './core/theme/theme.service';
import { ViadonauPreset } from './core/theme/viadonau.preset';
import {
  monitoringCollectionFixture,
  TEST_RUNTIME_CONFIG,
  waterLevelDetailFixture,
} from '../testing/fixtures';

describe('monitoring routes in Chromium', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-07-23T00:00:00Z')));

  afterEach(() => {
    http?.verify();
    fixture?.destroy();
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it.each([1280, 390])('centers the loading text below the spinner at %ipx', async (width) => {
    await page.viewport(width, 900);
    await renderRoute('/overview');
    const request = expectOverviewRequest();
    const spinner = page.getByRole('progressbar', { name: 'Lädt' });
    const label = page.getByText('Inhalte werden geladen', { exact: true });
    await expect.element(spinner).toBeVisible();
    await expect.element(label).toBeVisible();

    const spinnerBounds = spinner.element().getBoundingClientRect();
    const labelBounds = label.element().getBoundingClientRect();
    expect(labelBounds.top).toBeGreaterThan(spinnerBounds.bottom);
    expect(
      Math.abs(spinnerBounds.x + spinnerBounds.width / 2 - (labelBounds.x + labelBounds.width / 2)),
    ).toBeLessThan(1);

    request.flush(monitoringCollectionFixture());
    await expect.element(spinner).not.toBeInTheDocument();
    await expect
      .element(page.getByRole('gridcell', { name: 'Hauptpegel', exact: true }))
      .toBeVisible();
  });

  it('filters the desktop overview and opens the loaded detail route', async () => {
    await page.viewport(1280, 900);
    await renderRoute('/overview');

    expectOverviewRequest().flush(monitoringCollectionFixture());
    await expect.element(page.getByRole('heading', { name: 'Messreihen' })).toBeVisible();
    await expect
      .element(page.getByRole('gridcell', { name: 'Hauptpegel', exact: true }))
      .toBeVisible();
    page
      .getByRole('columnheader', { name: /^Messreihe/ })
      .element()
      .focus();
    await userEvent.keyboard('{Control>}{Enter}{/Control}');
    await page.getByRole('textbox', { name: 'Filterwert' }).fill('Hauptpegel');

    await expect.element(page.getByText('1 von 2 Messreihen', { exact: true })).toBeVisible();
    const reset = page.getByRole('button', { name: 'Zurücksetzen', exact: true });
    const grid = page.getByRole('region', { name: 'Messreihen und aktuelle Messwerte' });
    const resetBounds = reset.element().getBoundingClientRect();
    expect(resetBounds.bottom).toBeGreaterThan(grid.element().getBoundingClientRect().bottom);
    expect(
      reset
        .element()
        .contains(
          document.elementFromPoint(
            resetBounds.x + resetBounds.width / 2,
            resetBounds.y + resetBounds.height / 2,
          ),
        ),
    ).toBe(true);
    await reset.click();
    await expect.element(page.getByText('2 Messreihen', { exact: true })).toBeVisible();
    await page.getByRole('textbox', { name: 'Filterwert' }).fill('unbekannter Messpunkt');
    await expect.element(page.getByText('0 von 2 Messreihen', { exact: true })).toBeVisible();
    await reset.click();
    await expect.element(page.getByText('2 Messreihen', { exact: true })).toBeVisible();
    await page.getByRole('textbox', { name: 'Filterwert' }).fill('Hauptpegel');
    await expect.element(page.getByText('1 von 2 Messreihen', { exact: true })).toBeVisible();
    await userEvent.keyboard('{Escape}');
    await expect.element(page.getByRole('textbox', { name: 'Filterwert' })).not.toBeInTheDocument();
    await page.getByRole('button', { name: 'Messreihe öffnen: Hauptpegel, Wasserstand' }).click();

    (await detailRequest()).flush(waterLevelDetailFixture());
    (await intervalRequest('24h')).flush(intervalFixture());

    await expect.element(page.getByRole('heading', { name: 'Hauptpegel' })).toBeVisible();
    await expect.element(page.getByText('Wasserstand · Wien Brigittenau · Donau')).toBeVisible();
    expect(router.url).toBe('/overview/series-water-level');
  });

  it('keeps essential overview controls visible without mobile overflow', async () => {
    await page.viewport(390, 844);
    await renderRoute('/overview');

    expectOverviewRequest().flush(monitoringCollectionFixture());
    await expect
      .element(page.getByRole('gridcell', { name: 'Hauptpegel', exact: true }))
      .toBeVisible();
    await expect
      .element(page.getByRole('gridcell', { name: '312,5 cm', exact: true }))
      .toBeVisible();
    await expect
      .element(page.getByRole('columnheader', { name: /^Letzter Messwert/ }))
      .toBeVisible();
    await expect
      .element(page.getByRole('button', { name: 'Messreihe öffnen: Hauptpegel, Wasserstand' }))
      .toBeVisible();

    expect(page.getByRole('columnheader', { name: 'Messgröße' }).query()).toBeNull();
    expect(page.getByRole('columnheader', { name: 'Pegelstelle' }).query()).toBeNull();
    expect(page.getByRole('columnheader', { name: 'Letzte Aktivität' }).query()).toBeNull();

    const grid = page.getByRole('region', { name: 'Messreihen und aktuelle Messwerte' }).element();
    const gridViewport = grid.querySelector<HTMLElement>('.ag-body-horizontal-scroll-viewport');
    expect(gridViewport).not.toBeNull();
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(
      document.documentElement.clientWidth,
    );
    expect(grid.scrollWidth).toBeLessThanOrEqual(grid.clientWidth);
    expect(gridViewport!.scrollWidth).toBeLessThanOrEqual(gridViewport!.clientWidth);
  });

  it('removes the filter overlay when leaving the overview', async () => {
    await page.viewport(1280, 900);
    await renderRoute('/overview');
    expectOverviewRequest().flush(monitoringCollectionFixture());
    const header = page.getByRole('columnheader', { name: /^Messreihe/ });
    await expect.element(header).toBeVisible();
    header.element().focus();
    await userEvent.keyboard('{Control>}{Enter}{/Control}');
    const filter = page.getByRole('textbox', { name: 'Filterwert' });
    await expect.element(filter).toBeVisible();

    await router.navigateByUrl('/forbidden');
    await expect.element(filter).not.toBeInTheDocument();
    expect(document.querySelector('.ag-popup')).toBeNull();
  });

  it('paints detail history and reloads the selected range with its snapshot', async () => {
    await page.viewport(1280, 900);
    await renderRoute('/overview/series-water-level');

    (await detailRequest()).flush(waterLevelDetailFixture());
    (await intervalRequest('24h')).flush(
      intervalFixture(
        [bucketPoint('2026-07-22T08:00:00Z', 301), bucketPoint('2026-07-22T09:00:00Z', 312.5)],
        { from: '2026-07-22T00:00:00Z', to: '2026-07-23T00:00:00Z' },
      ),
    );

    const canvasLocator = page.getByRole('img', {
      name: 'Mittelwerte der ausgewählten Messreihe',
    });
    await expect.element(canvasLocator).toBeVisible();
    const canvas = canvasLocator.element() as HTMLCanvasElement;
    const initialCanvas = canvas.toDataURL();
    expect(paintedPixelCount(canvas)).toBeGreaterThan(0);

    await page.getByText('Information', { exact: true }).click();

    await page.getByRole('button', { name: 'Information zum Mittelungsintervall' }).hover();
    await expect
      .element(page.getByRole('tooltip'))
      .toHaveTextContent('Jeder Punkt ist das zeitgewichtete Mittel des vorherigen Intervalls.');

    await page.getByRole('switch', { name: 'RNW- und HSW-Referenzlinien anzeigen' }).click();
    await vi.waitFor(() => expect(canvas.toDataURL()).not.toBe(initialCanvas));

    await page.getByRole('combobox', { name: 'Zeitraum' }).click();
    await page.getByRole('option', { name: '7 Tage' }).click();
    (await intervalRequest('7d')).flush(
      intervalFixture(
        [bucketPoint('2026-07-16T08:00:00Z', 298), bucketPoint('2026-07-22T09:00:00Z', 312.5)],
        { from: '2026-07-16T00:00:00Z', to: '2026-07-23T00:00:00Z' },
      ),
    );
    await expect
      .element(
        page.getByText(
          '2 Mittelwerte · 670 Intervalle ohne vollständige Stützung · 15 Min. Raster · MEZ (UTC+1)',
          { exact: true },
        ),
      )
      .toBeVisible();

    await page.getByRole('button', { name: 'Messdaten aktualisieren' }).click();
    const refreshedDetail = await detailRequest();
    const refreshedHistory = await intervalRequest('7d');
    refreshedDetail.flush(
      waterLevelDetailFixture({ observedAt: '2026-07-22T10:05:00Z', value: 313.5 }),
    );
    refreshedHistory.flush(
      intervalFixture(
        [
          bucketPoint('2026-07-16T08:00:00Z', 298),
          bucketPoint('2026-07-20T08:00:00Z', 307),
          bucketPoint('2026-07-22T10:00:00Z', 313.5),
        ],
        { from: '2026-07-16T00:00:00Z', to: '2026-07-23T00:00:00Z' },
      ),
    );

    await expect.element(page.getByText('313,5', { exact: true })).toBeVisible();
    await expect
      .element(
        page.getByText(
          '3 Mittelwerte · 669 Intervalle ohne vollständige Stützung · 15 Min. Raster · MEZ (UTC+1)',
          { exact: true },
        ),
      )
      .toBeVisible();
  });

  it.each([
    { width: 1920, height: 1400 },
    { width: 1440, height: 900 },
    { width: 1280, height: 640 },
    { width: 768, height: 1200 },
    { width: 390, height: 1200 },
    { width: 320, height: 1200 },
  ])('fits detail at $width x $height', async ({ width, height }) => {
    await page.viewport(width, height);
    await renderRoute('/overview/series-water-level');
    await expect
      .element(page.getByRole('link', { name: 'Messreihen', exact: true }))
      .not.toBeInTheDocument();
    const window = { from: '2026-07-22T00:00:00Z', to: '2026-07-23T00:00:00Z' };
    (await detailRequest()).flush(waterLevelDetailFixture());
    (await intervalRequest('24h')).flush(
      intervalFixture(
        [
          bucketPoint('2026-07-22T08:00:00Z', 301, 60),
          bucketPoint('2026-07-22T14:00:00Z', 315.5, 60),
        ],
        window,
        '1h',
      ),
    );

    const canvas = page.getByRole('img', {
      name: 'Mittelwerte der ausgewählten Messreihe',
    });
    await expect.element(canvas).toBeVisible();
    const chartCanvas = canvas.element() as HTMLCanvasElement;
    await vi.waitFor(() => expect(paintedPixelCount(chartCanvas)).toBeGreaterThan(0));
    await expect
      .element(page.getByRole('heading', { name: 'Hauptpegel', exact: true }))
      .toBeVisible();
    const sidebar = page.getByRole('region', {
      name: 'Messreihe und Analyseeinstellungen',
    });
    const informationSummary = page.getByText('Information', { exact: true });
    const information = informationSummary.element().closest('details') as HTMLDetailsElement;
    const settings = page
      .getByRole('heading', { name: 'Anzeige' })
      .element()
      .closest('.ph-analysis-settings') as HTMLElement;
    const initialChartBounds = chartCanvas.getBoundingClientRect().toJSON();

    if (width >= 960) {
      const viewControls = page.getByRole('group', { name: 'Darstellung' });
      expect(viewControls.element().getBoundingClientRect().top).toBeCloseTo(
        sidebar.element().getBoundingClientRect().top,
        0,
      );
    }

    expect(information.open).toBe(false);
    expect(settings.getBoundingClientRect().bottom).toBeLessThanOrEqual(
      information.getBoundingClientRect().top,
    );
    await informationSummary.click();
    expect(information.open).toBe(true);
    await expect.element(page.getByText('312,5', { exact: true })).toBeVisible();

    const sidebarBounds = sidebar.element().getBoundingClientRect();
    const chartBounds = chartCanvas.getBoundingClientRect();
    expect(chartBounds.bottom).toBeLessThanOrEqual(
      chartCanvas.closest('ph-measurement-chart')!.getBoundingClientRect().bottom,
    );

    if (width >= 960) {
      expect(sidebarBounds.right).toBeLessThan(chartBounds.left);
      const scrollArea = sidebar.element() as HTMLElement;
      scrollArea.scrollTop = scrollArea.scrollHeight;
      if (height >= 1200) {
        expect(scrollArea.scrollHeight).toBe(scrollArea.clientHeight);
        expect(scrollArea.scrollTop).toBe(0);
      } else {
        await vi.waitFor(() => expect(scrollArea.scrollTop).toBeGreaterThan(0));
      }
      expect(information.getBoundingClientRect().bottom).toBeLessThanOrEqual(
        sidebarBounds.bottom + 1,
      );
      expect(chartCanvas.getBoundingClientRect().toJSON()).toEqual(initialChartBounds);
      expect(document.documentElement.scrollHeight).toBeLessThanOrEqual(
        document.documentElement.clientHeight,
      );

      // Menus must remain clickable outside the independently scrolling sidebar.
      await page.getByRole('combobox', { name: 'Zeitbasis' }).click();
      const optionLocator = page.getByRole('option', { name: 'UTC', exact: true });
      await expect.element(optionLocator).toBeVisible();
      const option = optionLocator.element();
      const optionBounds = option.getBoundingClientRect();
      expect(
        option.contains(
          document.elementFromPoint(
            optionBounds.x + optionBounds.width / 2,
            optionBounds.y + optionBounds.height / 2,
          ),
        ),
      ).toBe(true);
      await userEvent.keyboard('{Escape}');
      await expect.element(optionLocator).not.toBeInTheDocument();

      const main = page.getByRole('group', { name: 'Darstellung' }).element().parentElement!;
      const sidebarTop = scrollArea.scrollTop;
      main.scrollTop = main.scrollHeight;
      expect(chartCanvas.getBoundingClientRect().bottom).toBeLessThanOrEqual(
        main.getBoundingClientRect().bottom,
      );
      expect(scrollArea.scrollTop).toBe(sidebarTop);
    } else {
      expect(sidebarBounds.bottom).toBeLessThanOrEqual(chartBounds.top);
      expect(document.documentElement.scrollHeight).toBeGreaterThan(
        document.documentElement.clientHeight,
      );
    }
    expect(chartBounds.width).toBeLessThanOrEqual(width);
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(
      document.documentElement.clientWidth,
    );
  });

  it.each([
    { width: 390, theme: 'light' },
    { width: 1280, theme: 'dark' },
  ] as const)(
    'keeps the compact tooltip readable and on the same measurement at $width px in $theme mode',
    async ({ width, theme }) => {
      await page.viewport(width, 900);
      await renderRoute('/overview/series-water-level');
      const themeService = TestBed.inject(ThemeService);
      if (themeService.mode() !== theme) themeService.toggle();
      const points = Array.from({ length: 21 }, (_, index) =>
        bucketPoint(
          new Date(Date.UTC(2026, 6, 22, 8, index * 15)).toISOString(),
          index === 10 ? 330 : 302.724,
        ),
      );
      (await detailRequest()).flush(waterLevelDetailFixture());
      (await intervalRequest('24h')).flush(intervalFixture(points));

      const canvas = page.getByRole('img', {
        name: 'Mittelwerte der ausgewählten Messreihe',
      });
      await expect.element(canvas).toBeVisible();
      canvas.element().scrollIntoView({ block: 'center' });
      const chart = Chart.getChart(canvas.element() as HTMLCanvasElement)!;
      const markers = chart.getDatasetMeta(0).data;
      const spacing = markers[1].x - markers[0].x;

      // Beside a steep rise, the closest point in 2D is not the closest timestamp.
      for (const [index, offset, value, expectedTime] of [
        [10, -0.2, 302.724, '11:30'],
        [9, 0.2, 330, '11:15'],
        [0, 0.1, 330, '09:00'],
        [20, -0.1, 330, '14:00'],
      ] as const) {
        await canvas.hover({
          position: {
            x: markers[index].x + offset * spacing,
            y: chart.scales['y'].getPixelForValue(value),
          },
        });
        await vi.waitFor(() => {
          expect(chart.getActiveElements().map((point) => point.index)).toEqual([index]);
          expect(chart.tooltip?.dataPoints.map((point) => point.dataIndex)).toEqual([index]);
          expect(chart.tooltip?.dataPoints[0].parsed.y).toBe(points[index].mean);
          const title = chart.tooltip?.title[0] ?? '';
          expect(title).toContain(expectedTime);
          expect(title.match(/22\./g)).toHaveLength(1);
          expect(chart.tooltip?.body[0].lines).toEqual([`${index === 10 ? '330' : '302,724'} cm`]);
          expect(chart.tooltip?.footer).toEqual(['1 neuer Messwert']);
          expect(chart.tooltip!.x).toBeGreaterThanOrEqual(0);
          expect(chart.tooltip!.x + chart.tooltip!.width).toBeLessThanOrEqual(chart.width);
          expect(chart.tooltip!.y).toBeGreaterThanOrEqual(0);
          expect(chart.tooltip!.y + chart.tooltip!.height).toBeLessThanOrEqual(chart.height);
        });
      }
      await page.getByRole('heading', { name: 'Wasserstand (cm)' }).hover();
      await vi.waitFor(() => expect(chart.getActiveElements()).toHaveLength(0));
      expect(chart.tooltip?.opacity).toBe(0);
    },
  );

  it.each([
    { width: 1280, factor: 1 },
    { width: 390, factor: 0.0001 },
  ])(
    'keeps fractional discharge changes and axis labels visible at $width px',
    async ({ width, factor }) => {
      await page.viewport(width, 900);
      await renderRoute('/overview/series-water-level');
      const values = [1.08, 1.277, 1.15].map((value) => value * factor);
      (await detailRequest()).flush({
        ...waterLevelDetailFixture(),
        observedProperty: 'discharge',
        unit: 'm3/s',
        latestMeasurement: { observedAt: '2026-07-22T08:30:00Z', value: values[2] },
      });
      (await intervalRequest('24h')).flush({
        ...intervalFixture(
          values.map((value, index) =>
            bucketPoint(new Date(Date.UTC(2026, 6, 22, 8, index * 15)).toISOString(), value),
          ),
        ),
        unit: 'm3/s',
      });

      const canvas = page.getByRole('img', { name: 'Mittelwerte der ausgewählten Messreihe' });
      await expect.element(canvas).toBeVisible();
      const chartCanvas = canvas.element() as HTMLCanvasElement;
      const chart = Chart.getChart(chartCanvas)!;
      await vi.waitFor(() => expect(paintedPixelCount(chartCanvas)).toBeGreaterThan(0));
      const y = chart.scales['y'];
      expect(y.min).toBeLessThanOrEqual(Math.min(...values));
      expect(y.max).toBeGreaterThanOrEqual(Math.max(...values));
      expect(
        Math.abs(y.getPixelForValue(values[1]) - y.getPixelForValue(values[0])),
      ).toBeGreaterThan((chart.chartArea.bottom - chart.chartArea.top) / 2);
      expect(new Set(y.ticks.map((tick) => tick.label)).size).toBe(y.ticks.length);
      chart.getDatasetMeta(0).data.forEach((point, index) => {
        expect(point.y).toBeCloseTo(y.getPixelForValue(values[index]));
      });
    },
  );

  it('switches the chart tick alignment to UTC with the selected time basis', async () => {
    await page.viewport(1280, 900);
    await renderRoute('/overview/series-water-level');
    (await detailRequest()).flush(waterLevelDetailFixture());
    (await intervalRequest('24h')).flush(
      intervalFixture([bucketPoint('2026-07-22T10:00:00Z', 301)]),
    );
    await page.getByRole('combobox', { name: 'Zeitbasis' }).click();
    await page.getByRole('option', { name: 'UTC', exact: true }).click();
    const request = await intervalRequest('24h');
    expect(request.request.params.get('timeBasis')).toBe('UTC');
    request.flush({
      ...intervalFixture([bucketPoint('2026-07-22T10:00:00Z', 301)]),
      timeBasis: 'UTC',
    });
    const canvas = page.getByRole('img', {
      name: 'Mittelwerte der ausgewählten Messreihe',
    });
    await expect.element(canvas).toBeVisible();
    await expect
      .element(
        page.getByText(
          '1 Mittelwert · 95 Intervalle ohne vollständige Stützung · 15 Min. Raster · UTC',
          {
            exact: true,
          },
        ),
      )
      .toBeVisible();
  });

  async function renderRoute(url: string): Promise<void> {
    window.localStorage.clear();
    window.localStorage.setItem('pegelhub.theme', 'light');

    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes, withComponentInputBinding()),
        providePrimeNG({
          theme: { preset: ViadonauPreset, options: { darkModeSelector: '.ph-dark' } },
        }),
        { provide: RUNTIME_CONFIG, useValue: TEST_RUNTIME_CONFIG },
        {
          provide: AuthStateService,
          useValue: { userName: signal('Test Operator'), logout: vi.fn() },
        },
      ],
    });

    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await router.navigateByUrl(url);
    fixture.detectChanges();
    TestBed.tick();
  }

  function expectOverviewRequest(): TestRequest {
    return http.expectOne('/api/v1/monitoring/time-series?latestWithin=365d');
  }

  function detailRequest(): Promise<TestRequest> {
    return waitForRequest(
      (request) =>
        request.url === '/api/v1/monitoring/time-series/series-water-level' &&
        request.params.get('latestWithin') === '365d',
    );
  }

  function intervalRequest(range: string): Promise<TestRequest> {
    return waitForRequest(
      (request) =>
        request.url === '/api/v1/time-series/series-water-level/measurements/intervals' &&
        request.params.get('interval') === '15m' &&
        Date.parse(request.params.get('to')!) - Date.parse(request.params.get('from')!) ===
          (range === '24h' ? 1 : 7) * 24 * 60 * 60_000,
    );
  }

  async function waitForRequest(
    match: (request: TestRequest['request']) => boolean,
  ): Promise<TestRequest> {
    let request: TestRequest | undefined;

    await vi.waitFor(() => {
      const matches = http.match(match);
      expect(matches).toHaveLength(1);
      request = matches[0];
    });

    return request!;
  }
});

function bucketPoint(from: string, value: number, durationMinutes = 15) {
  const start = Date.parse(from);
  return {
    from,
    to: new Date(start + durationMinutes * 60_000).toISOString(),
    mean: value,
    observationCount: 1,
    supportedNanos: durationMinutes * 60 * 1_000_000_000,
    lastContributingObservedAt: from,
    windowStatus: 'closed',
    supportStatus: 'full',
  };
}

function intervalFixture(
  intervals: ReturnType<typeof bucketPoint>[] = [],
  window = { from: '2026-07-22T00:00:00Z', to: '2026-07-23T00:00:00Z' },
  interval = '15m',
) {
  const width = interval === '1h' ? 60 * 60_000 : 15 * 60_000;
  const byStart = new Map(intervals.map((item) => [Date.parse(item.from), item]));
  const complete = Array.from(
    { length: (Date.parse(window.to) - Date.parse(window.from)) / width },
    (_, index) => {
      const fromMs = Date.parse(window.from) + index * width;
      const from = new Date(fromMs).toISOString();
      return (
        byStart.get(fromMs) ?? {
          from,
          to: new Date(Date.parse(from) + width).toISOString(),
          mean: null,
          observationCount: 0,
          supportedNanos: 0,
          lastContributingObservedAt: null,
          windowStatus: 'closed',
          supportStatus: 'absent',
        }
      );
    },
  );
  return {
    timeSeriesId: 'series-water-level',
    ...window,
    interval,
    timeBasis: '+01:00',
    closedOnly: true,
    representation: 'canonical',
    unit: 'cm',
    method: 'time-weighted-step',
    computedAt: '2026-07-23T01:00:00Z',
    intervals: complete,
  };
}

function paintedPixelCount(canvas: HTMLCanvasElement): number {
  const context = canvas.getContext('2d');
  if (!context) return 0;

  const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
  let painted = 0;

  for (let index = 3; index < pixels.length; index += 4) {
    if (pixels[index] !== 0) painted += 1;
  }

  return painted;
}
