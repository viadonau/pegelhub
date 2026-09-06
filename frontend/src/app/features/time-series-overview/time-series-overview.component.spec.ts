import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import { monitoringCollectionFixture, TEST_RUNTIME_CONFIG } from '../../../testing/fixtures';
import { TimeSeriesOverviewComponent } from './time-series-overview.component';

describe('TimeSeriesOverviewComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TimeSeriesOverviewComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: RUNTIME_CONFIG, useValue: TEST_RUNTIME_CONFIG },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads and renders the operator-facing monitoring collection', async () => {
    const fixture = createComponent();

    expect(text(fixture)).toContain('Inhalte werden geladen');

    http
      .expectOne('/api/v1/monitoring/time-series?latestWithin=365d')
      .flush(monitoringCollectionFixture());
    TestBed.tick();
    fixture.detectChanges();

    await vi.waitFor(() => {
      const rendered = text(fixture);
      expect(rendered).toContain('2 Messreihen');
      expect(rendered).toContain('Hauptpegel');
      expect(rendered).toContain('312,5 cm');
      expect(rendered).toContain('Durchflussmesser');
      expect(rendered).toContain('Kein Messwert');

      const rowElements = fixture.nativeElement.querySelectorAll(
        '[role="row"]',
      ) as NodeListOf<Element>;
      const rows = Array.from(rowElements).map(
        (row) => row.textContent?.replace(/\s+/g, ' ').trim() ?? '',
      );
      expect(rows.findIndex((row) => row.includes('Durchflussmesser'))).toBeLessThan(
        rows.findIndex((row) => row.includes('Hauptpegel')),
      );
    });
  });

  it('shows the genuine empty state for an empty collection', async () => {
    const fixture = createComponent();

    http.expectOne('/api/v1/monitoring/time-series?latestWithin=365d').flush({ items: [] });
    TestBed.tick();
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(text(fixture)).toContain('0 Messreihen');
      expect(text(fixture)).toContain('Keine Messreihen vorhanden.');
    });
  });

  it('shows a useful error when the monitoring request fails', async () => {
    const fixture = createComponent();

    http
      .expectOne('/api/v1/monitoring/time-series?latestWithin=365d')
      .flush('unavailable', { status: 503, statusText: 'Service Unavailable' });
    await vi.waitFor(() => {
      TestBed.tick();
      fixture.detectChanges();
      expect(text(fixture)).toContain('Die Messreihenübersicht konnte nicht geladen werden.');
      expect(text(fixture)).not.toContain('Keine Messreihen vorhanden.');
    });
  });
});

function createComponent(): ComponentFixture<TimeSeriesOverviewComponent> {
  const fixture = TestBed.createComponent(TimeSeriesOverviewComponent);
  fixture.detectChanges();
  TestBed.tick();
  return fixture;
}

function text(fixture: ComponentFixture<unknown>): string {
  return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
}
