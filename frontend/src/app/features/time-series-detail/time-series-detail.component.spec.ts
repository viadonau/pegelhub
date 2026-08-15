import { signal, Signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeasurementApiService } from '../../core/api/measurement-api.service';
import { MeasurementBucketListDto } from '../../core/api/measurement.dto';
import { MonitoringApiService } from '../../core/api/monitoring-api.service';
import { MonitoringTimeSeriesDetailDto } from '../../core/api/monitoring.dto';
import { TimeSeriesDetailComponent } from './time-series-detail.component';

describe('TimeSeriesDetailComponent', () => {
  const selected = detail('series-w', 'water-level', 'cm');
  let snapshotResource: ReturnType<typeof fakeResource<MonitoringTimeSeriesDetailDto>>;
  let bucketsResource: ReturnType<typeof fakeResource<MeasurementBucketListDto>>;
  let monitoringApi: { timeSeriesDetailResource: ReturnType<typeof vi.fn> };
  let measurementsApi: { measurementBucketsResource: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    snapshotResource = fakeResource(selected);
    bucketsResource = fakeResource(emptyBuckets('series-w'));
    monitoringApi = { timeSeriesDetailResource: vi.fn(() => snapshotResource) };
    measurementsApi = { measurementBucketsResource: vi.fn(() => bucketsResource) };

    TestBed.configureTestingModule({
      imports: [TimeSeriesDetailComponent],
      providers: [
        provideRouter([]),
        { provide: MonitoringApiService, useValue: monitoringApi },
        { provide: MeasurementApiService, useValue: measurementsApi },
      ],
    });
  });

  it('renders the complete monitoring snapshot with station context', () => {
    const fixture = createComponent();
    const text = normalizedText(fixture);
    const heading = fixture.nativeElement.querySelector('h1');
    const headingContext = fixture.nativeElement.querySelector('.ph-time-series-heading-context');

    expect(text).toContain('Messreihe');
    expect(heading.textContent.trim()).toBe('Hauptpegel');
    expect(headingContext.textContent.trim()).toBe(
      'Wasserstand · Wien Brigittenau · 10001030 · Donau',
    );
    expect(text).toContain('312,5');
    expect(text).toContain('Organisationviadonau');
    expect(text).toContain('Einheitcm');
    expect(text).toContain('Messverlauf · Wasserstand (cm)');
  });

  it('starts the detail snapshot and bucket resources from the route id', () => {
    createComponent();
    const snapshotId = monitoringApi.timeSeriesDetailResource.mock.calls[0][0] as Signal<string>;
    const bucketId = measurementsApi.measurementBucketsResource.mock.calls[0][0] as Signal<string>;

    expect(monitoringApi.timeSeriesDetailResource).toHaveBeenCalledWith(expect.anything());
    expect(snapshotId()).toBe('series-w');
    expect(bucketId()).toBe('series-w');
    expect(measurementsApi.measurementBucketsResource).toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
    );
  });

  it('refreshes both the snapshot and chart buckets', () => {
    const fixture = createComponent();
    const refresh = fixture.nativeElement.querySelector(
      'button[aria-label="Messdaten aktualisieren"]',
    ) as HTMLButtonElement;

    refresh.click();

    expect(snapshotResource.reload).toHaveBeenCalledOnce();
    expect(bucketsResource.reload).toHaveBeenCalledOnce();
  });
});

function detail(id: string, observedProperty: string, unit: string): MonitoringTimeSeriesDetailDto {
  return {
    id,
    observedProperty,
    unit,
    externalCode: null,
    measuringPoint: {
      id: 'point-1',
      name: 'Hauptpegel',
      referenceLevel: null,
      referenceYear: 2020,
      riverKilometer: 1933.2,
      bank: 'right',
      rnw: null,
      mw: 315.5,
      hsw: null,
      hw100: null,
    },
    station: {
      id: 'station-1',
      stationNumber: '10001030',
      name: 'Wien Brigittenau',
      waterBody: 'Donau',
    },
    stationOwner: { id: 'owner-1', name: 'viadonau', shortName: null },
    latestMeasurement: { observedAt: '2026-07-22T20:00:00Z', value: 312.5 },
  };
}

function emptyBuckets(timeSeriesId: string): MeasurementBucketListDto {
  return { timeSeriesId, window: null, resolution: null, points: [] };
}

function createComponent(): ComponentFixture<TimeSeriesDetailComponent> {
  const fixture = TestBed.createComponent(TimeSeriesDetailComponent);
  fixture.componentRef.setInput('timeSeriesId', 'series-w');
  fixture.detectChanges();
  TestBed.flushEffects();
  fixture.detectChanges();
  return fixture;
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
