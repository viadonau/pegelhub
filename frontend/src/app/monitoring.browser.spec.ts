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
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from './app';
import { routes } from './app.routes';
import { AuthStateService } from './core/auth/auth-state.service';
import { RUNTIME_CONFIG } from './core/config/runtime-config';
import { ThemeService } from './core/theme/theme.service';
import { ViadonauPreset } from './core/theme/viadonau.preset';
import {
  measurementBucketsFixture,
  monitoringCollectionFixture,
  TEST_RUNTIME_CONFIG,
  waterLevelDetailFixture,
} from '../testing/fixtures';

describe('monitoring routes in Chromium', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let router: Router;

  afterEach(() => {
    http?.verify();
    fixture?.destroy();
    window.localStorage.clear();
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
    (await bucketRequest('24h')).flush(measurementBucketsFixture());

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
    (await bucketRequest('24h')).flush(
      measurementBucketsFixture([
        bucketPoint('2026-07-22T08:00:00Z', 301),
        bucketPoint('2026-07-22T09:00:00Z', 312.5),
      ]),
    );

    const canvasLocator = page.getByRole('img', {
      name: 'Gemittelter Messverlauf der ausgewählten Messreihe',
    });
    await expect.element(canvasLocator).toBeVisible();
    const canvas = canvasLocator.element() as HTMLCanvasElement;
    const initialCanvas = canvas.toDataURL();
    expect(paintedPixelCount(canvas)).toBeGreaterThan(0);

    await page.getByRole('switch', { name: 'RNW- und HSW-Referenzlinien anzeigen' }).click();
    await vi.waitFor(() => expect(canvas.toDataURL()).not.toBe(initialCanvas));

    await page.getByRole('combobox', { name: 'Zeitraum auswählen' }).click();
    await page.getByRole('option', { name: '7 Tage' }).click();
    (await bucketRequest('7d')).flush(
      measurementBucketsFixture([
        bucketPoint('2026-07-16T08:00:00Z', 298),
        bucketPoint('2026-07-22T09:00:00Z', 312.5),
      ]),
    );
    await expect.element(page.getByText('2 Datenpunkte', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Messdaten aktualisieren' }).click();
    const refreshedDetail = await detailRequest();
    const refreshedHistory = await bucketRequest('7d');
    refreshedDetail.flush(
      waterLevelDetailFixture({ observedAt: '2026-07-22T10:05:00Z', value: 313.5 }),
    );
    refreshedHistory.flush(
      measurementBucketsFixture([
        bucketPoint('2026-07-16T08:00:00Z', 298),
        bucketPoint('2026-07-20T08:00:00Z', 307),
        bucketPoint('2026-07-22T10:00:00Z', 313.5),
      ]),
    );

    await expect.element(page.getByText('313,5', { exact: true })).toBeVisible();
    await expect.element(page.getByText('3 Datenpunkte', { exact: true })).toBeVisible();
  });

  it.each([1280, 390])(
    'keeps the tooltip and marker on the same measurement at %ipx',
    async (width) => {
      await page.viewport(width, 900);
      await renderRoute('/overview/series-water-level');
      const points = Array.from({ length: 21 }, (_, index) =>
        bucketPoint(
          new Date(Date.UTC(2026, 6, 22, 8, index * 15)).toISOString(),
          index === 10 ? 330 : 302.724,
        ),
      );
      (await detailRequest()).flush(waterLevelDetailFixture());
      (await bucketRequest('24h')).flush(measurementBucketsFixture(points));

      const canvas = page.getByRole('img', {
        name: 'Gemittelter Messverlauf der ausgewählten Messreihe',
      });
      await expect.element(canvas).toBeVisible();
      canvas.element().scrollIntoView({ block: 'center' });
      const chart = Chart.getChart(canvas.element() as HTMLCanvasElement)!;
      const markers = chart.getDatasetMeta(0).data;
      const spacing = markers[1].x - markers[0].x;

      // Beside a steep rise, the closest point in 2D is not the closest timestamp.
      for (const [index, offset, value] of [
        [10, -0.2, 302.724],
        [9, 0.2, 330],
        [0, 0.1, 330],
        [20, -0.1, 330],
      ]) {
        await canvas.hover({
          position: {
            x: markers[index].x + offset * spacing,
            y: chart.scales['y'].getPixelForValue(value),
          },
        });
        await vi.waitFor(() => {
          expect(chart.getActiveElements().map((point) => point.index)).toEqual([index]);
          expect(chart.tooltip?.dataPoints.map((point) => point.dataIndex)).toEqual([index]);
          expect(chart.tooltip?.dataPoints[0].parsed.y).toBe(points[index].value);
          expect(chart.tooltip?.title).toEqual([chart.data.labels![index]]);
          expect(chart.tooltip?.body[0].lines).toEqual([index === 10 ? '330 cm' : '302,724 cm']);
        });
      }
      await page.getByRole('heading', { name: 'Messverlauf · Wasserstand (cm)' }).hover();
      await vi.waitFor(() => expect(chart.getActiveElements()).toHaveLength(0));
      expect(chart.tooltip?.opacity).toBe(0);
    },
  );

  async function renderRoute(url: string): Promise<void> {
    window.localStorage.clear();
    const themeMode = signal<'light' | 'dark'>('light');

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
        {
          provide: ThemeService,
          useValue: {
            mode: themeMode,
            toggleLabel: signal('Dunkles Erscheinungsbild verwenden'),
            toggleIcon: signal('pi pi-moon'),
            toggle: vi.fn(),
          },
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

  function bucketRequest(range: string): Promise<TestRequest> {
    return waitForRequest(
      (request) =>
        request.url === '/api/v1/time-series/series-water-level/measurements/buckets' &&
        request.params.get('last') === range &&
        request.params.get('maxPoints') === '240',
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

function bucketPoint(from: string, value: number) {
  return {
    from,
    to: from,
    value,
    sampleCount: 1,
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
