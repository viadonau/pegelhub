import { Injector, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeasuringPointApiService } from '../../core/api/measuring-point-api.service';
import { MeasurementApiService } from '../../core/api/measurement-api.service';
import {
  EMPTY_MEASUREMENT_BUCKET_LIST,
  EMPTY_MEASUREMENT_LIST,
} from '../../core/api/measurement.dto';
import { StationApiService } from '../../core/api/station-api.service';
import { TimeSeriesApiService } from '../../core/api/time-series-api.service';
import { TimeSeriesDto } from '../../core/api/time-series.dto';
import { TimeSeriesDetailComponent } from './time-series-detail.component';

describe('TimeSeriesDetailComponent', () => {
  const selectedTimeSeries = timeSeries('series-w', 'water-level', 'cm');

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TimeSeriesDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: MeasuringPointApiService,
          useValue: {
            measuringPointResource: vi.fn(() =>
              fakeResource({
                id: 'point-1',
                stationId: 'station-1',
                name: 'Hauptpegel',
                bank: 'right',
                riverKilometer: 1933.2,
                referenceYear: 2020,
                mw: 315.5,
              }),
            ),
          },
        },
        {
          provide: StationApiService,
          useValue: {
            stationResource: vi.fn(() =>
              fakeResource({
                id: 'station-1',
                ownerId: 'owner-1',
                stationNumber: '10001030',
                name: 'Wien Brigittenau',
                waterBody: 'Donau',
              }),
            ),
            stationOwnerResource: vi.fn(() => fakeResource({ id: 'owner-1', name: 'viadonau' })),
          },
        },
        {
          provide: TimeSeriesApiService,
          useValue: {
            timeSeriesResource: vi.fn(() => fakeResource(selectedTimeSeries)),
          },
        },
        {
          provide: MeasurementApiService,
          useValue: {
            latestMeasurementResource: vi.fn(() =>
              fakeResource({
                ...EMPTY_MEASUREMENT_LIST,
                timeSeriesId: 'series-w',
                measurements: [{ observedAt: '2026-07-22T20:00:00Z', value: 312.5 }],
              }),
            ),
            measurementBucketsResource: vi.fn(() =>
              fakeResource({
                ...EMPTY_MEASUREMENT_BUCKET_LIST,
                timeSeriesId: 'series-w',
              }),
            ),
          },
        },
      ],
    });
  });

  it('renders the selected series with its measuring-point and station context', () => {
    const fixture = createComponent();
    const text = normalizedText(fixture);
    const heading = fixture.nativeElement.querySelector('h1');
    const headingContext = fixture.nativeElement.querySelector('.ph-time-series-heading-context');
    const metadata = fixture.nativeElement.querySelector('ph-time-series-metadata');
    const chart = fixture.nativeElement.querySelector('ph-time-series-measurements');

    expect(text).toContain('Messreihe');
    expect(heading.textContent.trim()).toBe('Hauptpegel');
    expect(headingContext.textContent.trim()).toBe(
      'Wasserstand · Wien Brigittenau · 10001030 · Donau',
    );
    expect(text).toContain('312,5');
    expect(text).toContain('Organisationviadonau');
    expect(text).toContain('Einheitcm');
    expect(text).toContain('Messverlauf · Wasserstand (cm)');
    expect(fixture.nativeElement.querySelector('[role="tablist"]')).toBeNull();
    expect(metadata.compareDocumentPosition(chart) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('uses the route time-series id directly without rendering sibling navigation', () => {
    const api = TestBed.inject(TimeSeriesApiService);
    const fixture = createComponent();

    expect(api.timeSeriesResource).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.querySelector('ph-time-series-selector')).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('ph-time-series-measurements')).toHaveLength(1);
  });

  it('creates resources in the detail component injector', () => {
    const api = TestBed.inject(TimeSeriesApiService);
    const fixture = createComponent();
    const resourceInjector = fixture.componentRef.injector.get(Injector);

    expect(resourceInjector).not.toBe(TestBed.inject(Injector));
    expect(api.timeSeriesResource).toHaveBeenCalledWith(expect.anything(), resourceInjector);
  });
});

function createComponent(): ComponentFixture<TimeSeriesDetailComponent> {
  const fixture = TestBed.createComponent(TimeSeriesDetailComponent);
  fixture.componentRef.setInput('timeSeriesId', 'series-w');
  fixture.detectChanges();
  TestBed.flushEffects();
  fixture.detectChanges();

  return fixture;
}

function timeSeries(id: string, observedProperty: string, unit: string): TimeSeriesDto {
  return {
    id,
    measuringPointId: 'point-1',
    observedProperty,
    unit,
  };
}

function fakeResource<T>(initialValue: T) {
  return {
    value: signal(initialValue),
    hasValue: () => true,
    status: signal('resolved'),
    isLoading: signal(false),
    reload: vi.fn(),
  };
}

function normalizedText(fixture: ComponentFixture<unknown>): string {
  return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
}
