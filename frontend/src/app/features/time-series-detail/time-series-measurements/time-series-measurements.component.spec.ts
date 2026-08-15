import { signal, type Signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import { MeasurementBucketListDto } from '../../../core/api/measurement.dto';
import { MemoryStorage } from '../../../../testing/memory-storage';
import { PhMeasurementHistoryToolbarComponent } from '../measurement-history-toolbar/measurement-history-toolbar.component';
import { PhTimeSeriesMeasurementsComponent } from './time-series-measurements.component';

describe('PhTimeSeriesMeasurementsComponent', () => {
  let buckets: ReturnType<typeof fakeResource<MeasurementBucketListDto>>;
  let measurementsApi: { measurementBucketsResource: ReturnType<typeof vi.fn> };
  let storage: Storage;

  beforeEach(() => {
    storage = new MemoryStorage();
    Object.defineProperty(window, 'localStorage', { configurable: true, value: storage });
    buckets = fakeResource(emptyBuckets('series-1'));
    measurementsApi = { measurementBucketsResource: vi.fn(() => buckets) };

    TestBed.configureTestingModule({
      imports: [PhTimeSeriesMeasurementsComponent],
      providers: [{ provide: MeasurementApiService, useValue: measurementsApi }],
    });
  });

  it('owns the ranged bucket resource for its time series', () => {
    const fixture = createComponent();
    const id = measurementsApi.measurementBucketsResource.mock.calls[0][0] as Signal<string>;
    const range = measurementsApi.measurementBucketsResource.mock.calls[0][1] as Signal<string>;

    expect(id()).toBe('series-1');
    expect(range()).toBe('24h');
    expect(normalizedText(fixture)).toContain('Messverlauf · Wasserstand (cm)');
  });

  it('reloads buckets and asks the page to refresh its snapshot', () => {
    const fixture = createComponent();
    const refresh = vi.fn();
    fixture.componentInstance.refresh.subscribe(refresh);

    clickRefresh(fixture);

    expect(buckets.reload).toHaveBeenCalledOnce();
    expect(refresh).toHaveBeenCalledOnce();
  });

  it('reloads only buckets when the range changes', () => {
    const fixture = createComponent();
    const range = measurementsApi.measurementBucketsResource.mock.calls[0][1] as Signal<string>;
    const toolbar = fixture.debugElement.query(By.directive(PhMeasurementHistoryToolbarComponent))
      .componentInstance as PhMeasurementHistoryToolbarComponent;

    toolbar.rangeChange.emit('7d');
    fixture.detectChanges();

    expect(range()).toBe('7d');
    expect(buckets.reload).not.toHaveBeenCalled();
  });

  it('does not read an errored bucket response', () => {
    const fixture = createComponent();
    buckets.status.set('error');

    expect(() => fixture.detectChanges()).not.toThrow();
    expect(normalizedText(fixture)).toContain('Der Messverlauf konnte nicht geladen werden.');
  });

  it('offers and persists reference-level visibility when references are available', () => {
    const fixture = createComponent();
    fixture.componentRef.setInput('referenceLines', [
      { label: 'RNW 2020', value: 162, tone: 'lower' },
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector(
      '#ph-reference-levels-toggle',
    ) as HTMLInputElement;
    expect(toggle).not.toBeNull();
    toggle.click();
    fixture.detectChanges();
    expect(storage.getItem('pegelhub.chart.referenceLevels')).toBe('true');
  });
});

function createComponent(): ComponentFixture<PhTimeSeriesMeasurementsComponent> {
  const fixture = TestBed.createComponent(PhTimeSeriesMeasurementsComponent);
  fixture.componentRef.setInput('timeSeriesId', 'series-1');
  fixture.componentRef.setInput('observedProperty', 'water-level');
  fixture.componentRef.setInput('unit', 'cm');
  fixture.detectChanges();
  return fixture;
}

function clickRefresh(fixture: ComponentFixture<unknown>): void {
  const refresh = fixture.nativeElement.querySelector(
    'button[aria-label="Messdaten aktualisieren"]',
  ) as HTMLButtonElement;
  refresh.click();
}

function emptyBuckets(timeSeriesId: string): MeasurementBucketListDto {
  return { timeSeriesId, window: null, resolution: null, points: [] };
}

function fakeResource<T>(initialValue: T) {
  const valueState = signal(initialValue);
  const status = signal('resolved');
  const value = Object.assign(
    () => {
      if (status() === 'error') {
        throw new Error('Resource value read while errored');
      }
      return valueState();
    },
    { set: (nextValue: T) => valueState.set(nextValue) },
  );

  return {
    value,
    hasValue: () => status() !== 'error',
    status,
    isLoading: signal(false),
    reload: vi.fn(),
  };
}

function normalizedText(fixture: ComponentFixture<unknown>): string {
  return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
}
