import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { page, userEvent } from 'vitest/browser';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from './app';
import { routes } from './app.routes';
import { AuthStateService } from './core/auth/auth-state.service';
import { RUNTIME_CONFIG } from './core/config/runtime-config';
import { ThemeService } from './core/theme/theme.service';
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

  async function renderRoute(url: string): Promise<void> {
    window.localStorage.clear();
    const themeMode = signal<'light' | 'dark'>('light');

    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes, withComponentInputBinding()),
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
