import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import {
  measurementBucketsFixture,
  TEST_RUNTIME_CONFIG,
  waterLevelDetailFixture,
} from '../../../testing/fixtures';
import { TimeSeriesDetailComponent } from './time-series-detail.component';

describe('TimeSeriesDetailComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TimeSeriesDetailComponent],
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

  it('renders an inactive water-level snapshot and its empty history', async () => {
    const fixture = createComponent();

    expect(text(fixture)).toContain('Inhalte werden geladen');

    expectDetailRequest().flush(waterLevelDetailFixture());
    await waitForHistory(fixture);

    expectBucketRequest().flush(measurementBucketsFixture());
    TestBed.tick();
    fixture.detectChanges();

    await vi.waitFor(() => {
      const rendered = text(fixture);
      expect(rendered).toContain('Hauptpegel');
      expect(rendered).toContain('Wasserstand · Wien Brigittenau · Donau');
      expect(rendered).toContain('Inaktiv');
      expect(rendered).toContain('312,5cm');
      expect(rendered).toContain('Organisationviadonau');
      expect(rendered).toContain('Uferrechts');
      expect(rendered).toContain('RNW 2020162 cm');
      expect(rendered).toContain(
        'Für Wasserstand sind in diesem Zeitraum keine Messwerte vorhanden.',
      );
    });
    expect(
      fixture.nativeElement.querySelector(
        'input[aria-label="RNW- und HSW-Referenzlinien anzeigen"]',
      ),
    ).not.toBeNull();
    expect(TestBed.inject(Title).getTitle()).toBe('Hauptpegel · Wasserstand · PegelHub');
  });

  it('keeps the loaded snapshot usable when bucket history fails', async () => {
    const fixture = createComponent();

    expectDetailRequest().flush(waterLevelDetailFixture());
    await waitForHistory(fixture);
    expectBucketRequest().flush('unavailable', {
      status: 503,
      statusText: 'Service Unavailable',
    });

    await vi.waitFor(() => {
      TestBed.tick();
      fixture.detectChanges();
      const rendered = text(fixture);
      expect(rendered).toContain('Hauptpegel');
      expect(rendered).toContain('312,5cm');
      expect(rendered).toContain('Der Messverlauf konnte nicht geladen werden.');
      expect(rendered).not.toContain('Die Messreihe konnte nicht geladen werden.');
    });
  });

  it('shows a detail error without starting a bucket request', async () => {
    const fixture = createComponent();

    expectDetailRequest().flush('unavailable', {
      status: 503,
      statusText: 'Service Unavailable',
    });

    await vi.waitFor(() => {
      TestBed.tick();
      fixture.detectChanges();
      expect(text(fixture)).toContain('Die Messreihe konnte nicht geladen werden.');
    });
    http.expectNone(
      (request) => request.url === '/api/v1/time-series/series-water-level/measurements/buckets',
    );
  });

  function expectDetailRequest(): TestRequest {
    return http.expectOne('/api/v1/monitoring/time-series/series-water-level?latestWithin=365d');
  }

  function expectBucketRequest(): TestRequest {
    return http.expectOne(
      (request) =>
        request.url === '/api/v1/time-series/series-water-level/measurements/buckets' &&
        request.params.get('last') === '24h' &&
        request.params.get('maxPoints') === '240',
    );
  }
});

function createComponent(): ComponentFixture<TimeSeriesDetailComponent> {
  const fixture = TestBed.createComponent(TimeSeriesDetailComponent);
  fixture.componentRef.setInput('timeSeriesId', 'series-water-level');
  fixture.detectChanges();
  TestBed.tick();
  return fixture;
}

function text(fixture: ComponentFixture<unknown>): string {
  return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
}

async function waitForHistory(fixture: ComponentFixture<unknown>): Promise<void> {
  await vi.waitFor(() => {
    TestBed.tick();
    fixture.detectChanges();
    expect(text(fixture)).toContain('Messverlauf · Wasserstand (cm)');
  });
}
