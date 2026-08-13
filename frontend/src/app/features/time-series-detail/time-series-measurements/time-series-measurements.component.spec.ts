import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import {
  EMPTY_MEASUREMENT_BUCKET_LIST,
  EMPTY_MEASUREMENT_LIST,
} from '../../../core/api/measurement.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import { MemoryStorage } from '../../../../testing/memory-storage';
import { TimeSeriesDetailMeasurementsStore } from '../data-access/time-series-detail-measurements.store';

import { PhTimeSeriesMeasurementsComponent } from './time-series-measurements.component';

const TIME_SERIES: TimeSeriesDto = {
  id: 'series-1',
  measuringPointId: 'point-1',
  observedProperty: 'water-level',
  unit: 'cm',
};

describe('PhTimeSeriesMeasurementsComponent', () => {
  let latest: ReturnType<typeof fakeResource>;
  let buckets: ReturnType<typeof fakeResource>;
  let storage: Storage;
  let measurementsApi: {
    latestMeasurementResource: ReturnType<typeof vi.fn>;
    measurementBucketsResource: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    storage = new MemoryStorage();
    Object.defineProperty(window, 'localStorage', { configurable: true, value: storage });
    latest = fakeResource(EMPTY_MEASUREMENT_LIST);
    buckets = fakeResource(EMPTY_MEASUREMENT_BUCKET_LIST);
    measurementsApi = {
      latestMeasurementResource: vi.fn(() => latest),
      measurementBucketsResource: vi.fn(() => buckets),
    };

    TestBed.configureTestingModule({
      providers: [
        TimeSeriesDetailMeasurementsStore,
        { provide: MeasurementApiService, useValue: measurementsApi },
      ],
    });
  });

  it('renders the chart workflow without requesting or displaying raw measurements', () => {
    const fixture = createComponent();
    const text = normalizedText(fixture);

    expect(text).toContain('Messverlauf');
    expect(text).not.toContain('Einzelmesswerte');
    expect(measurementsApi.latestMeasurementResource).toHaveBeenCalledOnce();
    expect(measurementsApi.measurementBucketsResource).toHaveBeenCalledOnce();
  });

  it('reloads the current value and chart from one refresh action', () => {
    const fixture = createComponent();
    const refresh = fixture.nativeElement.querySelector(
      'button[aria-label="Messdaten aktualisieren"]',
    ) as HTMLButtonElement;

    refresh.click();

    expect(latest.reload).toHaveBeenCalledOnce();
    expect(buckets.reload).toHaveBeenCalledOnce();
  });

  it('never exposes a retained latest reading for another time series', () => {
    const store = TestBed.inject(TimeSeriesDetailMeasurementsStore);
    store.setTimeSeries(TIME_SERIES);
    latest.value.set({
      ...EMPTY_MEASUREMENT_LIST,
      timeSeriesId: TIME_SERIES.id,
      measurements: [{ observedAt: '2026-07-19T10:00:00Z', value: 312.5 }],
    });
    TestBed.flushEffects();

    expect(store.latestReading()?.value).toBe('312,5');

    store.setTimeSeries({
      ...TIME_SERIES,
      id: 'series-2',
      observedProperty: 'discharge',
      unit: 'm³/s',
    });

    expect(store.latestReading()).toBeNull();
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
  TestBed.inject(TimeSeriesDetailMeasurementsStore).setTimeSeries(TIME_SERIES);

  const fixture = TestBed.createComponent(PhTimeSeriesMeasurementsComponent);
  fixture.detectChanges();

  return fixture;
}

function fakeResource<T>(initialValue: T) {
  return {
    value: signal(initialValue),
    status: signal('resolved'),
    isLoading: signal(false),
    reload: vi.fn(),
  };
}

function normalizedText(fixture: ComponentFixture<unknown>): string {
  return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
}
