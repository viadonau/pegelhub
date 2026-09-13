import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { page } from 'vitest/browser';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { AuthStateService } from './core/auth/auth-state.service';
import { RUNTIME_CONFIG } from './core/config/runtime-config';
import { ThemeService } from './core/theme/theme.service';
import { TEST_RUNTIME_CONFIG } from '../testing/fixtures';
import { providePrimeNG } from 'primeng/config';
import { ViadonauPreset } from './core/theme/viadonau.preset';

describe('operational modules in Chromium', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;

  const empty = { items: [], offset: 0, limit: 25, total: 0 };
  const config = {
    name: 'Donau QA',
    enabled: false,
    sources: ['input'],
    output: null,
    rules: { range: null, jumpThreshold: null, frozenSeconds: null, maxDeviation: null },
    lookbackSeconds: 1200,
    intervalSeconds: 60,
    destinations: [],
  };

  afterEach(() => {
    http?.verify();
    fixture?.destroy();
    document.documentElement.classList.remove('ph-dark');
  });

  it('gates configuration routes without granting monitoring users administration', async () => {
    await render('/quality/profiles', false);

    expect(TestBed.inject(Router).url).toBe('/forbidden');
    expect(page.getByRole('link', { name: 'Benachrichtigungen' }).query()).toBeNull();
    http.expectNone((request) => request.url.startsWith('/api/'));
  });

  it('keeps profile editing available when measurement reads fail and refreshes ownership after saving', async () => {
    await page.viewport(1280, 900);
    await render('/quality/profiles');
    http.expectOne('/api/v1/quality/profiles').flush([
      {
        id: 'profile',
        configuration: config,
        nextRunAt: null,
        currentRunId: null,
        runRequested: false,
      },
    ]);
    http.expectOne('/api/v1/quality/status').flush({ enabled: false });
    http.expectOne('/api/v1/time-series').flush([
      {
        id: 'input',
        status: 'active',
        observedProperty: 'water-level',
        unit: 'cm',
        sourceAssignment: null,
      },
    ]);
    http.expectOne('/api/v1/notifications/destinations').flush([]);
    http
      .expectOne('/api/v1/monitoring/time-series?latestWithin=365d')
      .flush({}, { status: 503, statusText: 'Unavailable' });

    await page.getByRole('button', { name: 'Profil bearbeiten', exact: true }).click();
    await expect
      .element(page.getByRole('textbox', { name: 'Name', exact: true }))
      .toHaveValue('Donau QA');

    await page.getByRole('textbox', { name: 'Name', exact: true }).fill('Donau Prüfung');
    await page.getByRole('group', { name: 'Sprung', exact: true }).getByRole('checkbox').click();
    await page.getByRole('button', { name: 'Speichern', exact: true }).click();
    await expect
      .element(
        page.getByText(
          'Bitte Quellen, Zeitfenster und aktivierte Regeln vollständig und gültig ausfüllen.',
        ),
      )
      .toBeVisible();
    http.expectNone((request) => request.method === 'PUT');

    await page.getByRole('spinbutton', { name: 'Maximale Änderung' }).fill('0');
    await page.screenshot({ path: '__screenshots__/quality-desktop.png' });
    await page.getByRole('button', { name: 'Speichern', exact: true }).click();
    const save = http.expectOne('/api/v1/quality/profiles/profile');
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual({
      ...config,
      name: 'Donau Prüfung',
      rules: { ...config.rules, jumpThreshold: 0 },
    });

    save.flush({ id: 'profile', configuration: { ...config, name: 'Donau Prüfung' } });
    await vi.waitFor(() => {
      http.expectOne('/api/v1/quality/profiles').flush([]);
      http.expectOne('/api/v1/time-series').flush([]);
    });
    await expect.element(page.getByText('Profil gespeichert.', { exact: true })).toBeVisible();
  });

  it.each([
    {
      transport: 'SMTP',
      mail: { from: 'ph@example.test', recipients: ['operator@example.test'], signature: null },
      snmp: null,
    },
    {
      transport: 'SNMP',
      mail: null,
      snmp: {
        host: 'receiver.example.test',
        port: 162,
        version: 'v3',
        trapOid: '1.3.6.1.6.3.1.1.5.1',
        messageOid: '1.3.6.1.4.1.65815.1.1',
        enterpriseOid: null,
      },
    },
  ])(
    'can disable a $transport destination after credential removal without changing its route',
    async (route) => {
      await page.viewport(1280, 900);
      await render('/notifications');
      const original = { name: 'Bereitschaft', enabled: true, credentialRef: 'removed', ...route };
      http.expectOne('/api/v1/notifications/status').flush({ enabled: true, credentials: [] });
      http
        .expectOne('/api/v1/notifications/destinations')
        .flush([{ id: 'destination', configuration: original }]);
      http.expectOne('/api/v1/notifications/deliveries?offset=0&limit=25').flush(empty);

      await page.getByRole('button', { name: 'Ziel bearbeiten', exact: true }).click();
      await page.getByRole('checkbox', { name: 'Aktiv', exact: true }).click();
      await page.getByRole('button', { name: 'Speichern', exact: true }).click();
      const save = http.expectOne('/api/v1/notifications/destinations/destination');
      expect(save.request.method).toBe('PUT');
      expect(save.request.body).toEqual({ ...original, enabled: false });

      save.flush({ id: 'destination', configuration: save.request.body });
      await vi.waitFor(() => {
        http.expectOne('/api/v1/notifications/destinations').flush([]);
        http.expectOne('/api/v1/notifications/deliveries?offset=0&limit=25').flush(empty);
      });
      await expect
        .element(page.getByText('Benachrichtigungsziel gespeichert.', { exact: true }))
        .toBeVisible();
    },
  );

  it('shows empty history and a failed refresh without mobile overflow', async () => {
    await page.viewport(390, 844);
    await render('/quality');
    http.expectOne('/api/v1/quality/runs?offset=0&limit=25').flush(empty);
    await expect.element(page.getByText('Noch keine Prüfläufe vorhanden.')).toBeVisible();

    await page.getByRole('button', { name: 'Prüfläufe aktualisieren' }).click();
    http
      .expectOne('/api/v1/quality/runs?offset=0&limit=25')
      .flush({}, { status: 500, statusText: 'Failure' });
    await expect.element(page.getByText('Prüfläufe konnten nicht geladen werden.')).toBeVisible();
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(
      document.documentElement.clientWidth,
    );
  });

  it('queues a test using only a configured destination identifier in dark mobile view', async () => {
    await page.viewport(390, 844);
    await render('/notifications');
    document.documentElement.classList.add('ph-dark');
    http.expectOne('/api/v1/notifications/status').flush({ enabled: true, credentials: [] });
    http.expectOne('/api/v1/notifications/destinations').flush([
      {
        id: 'mail',
        configuration: {
          name: 'Bereitschaft',
          enabled: true,
          transport: 'SMTP',
          credentialRef: 'relay',
          mail: { from: 'ph@example.test', recipients: ['operator@example.test'], signature: null },
          snmp: null,
        },
      },
    ]);
    http.expectOne('/api/v1/notifications/deliveries?offset=0&limit=25').flush(empty);

    await page.getByRole('button', { name: 'Testnachricht senden' }).click();
    const send = http.expectOne('/api/v1/notifications');
    expect(send.request.body).toEqual({
      destinations: ['mail'],
      subject: 'PH Testnachricht',
      body: 'Testnachricht aus PegelHub.',
    });

    send.flush({ requestId: 'request' }, { status: 202, statusText: 'Accepted' });
    await vi.waitFor(() =>
      http.expectOne('/api/v1/notifications/deliveries?offset=0&limit=25').flush(empty),
    );
    await expect.element(page.getByText('Testnachricht eingereiht: request')).toBeVisible();

    await page.screenshot({ path: '__screenshots__/notifications-mobile-dark.png' });
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(
      document.documentElement.clientWidth,
    );
  });

  async function render(url: string, admin = true) {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        providePrimeNG({
          theme: { preset: ViadonauPreset, options: { darkModeSelector: '.ph-dark' } },
        }),
        provideRouter(routes),
        { provide: RUNTIME_CONFIG, useValue: TEST_RUNTIME_CONFIG },
        {
          provide: AuthStateService,
          useValue: {
            userName: signal('Operator'),
            isAdmin: signal(admin),
            canMonitor: signal(true),
            logout: vi.fn(),
          },
        },
        {
          provide: ThemeService,
          useValue: {
            mode: signal('light'),
            toggleLabel: signal('Theme'),
            toggleIcon: signal('pi pi-moon'),
            toggle: vi.fn(),
          },
        },
      ],
    });

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();
    TestBed.tick();
  }
});
