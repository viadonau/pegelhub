import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MonitoringApiService } from '../../core/api/monitoring-api.service';
import { MonitoringTimeSeriesCollectionDto } from '../../core/api/monitoring.dto';
import { PhTimeSeriesGridComponent } from './time-series-grid/time-series-grid.component';
import { TimeSeriesOverviewComponent } from './time-series-overview.component';

describe('TimeSeriesOverviewComponent', () => {
  let resource: ReturnType<typeof fakeResource<MonitoringTimeSeriesCollectionDto>>;

  beforeEach(() => {
    resource = fakeResource({
      items: [
        {
          id: 'series-w',
          observedProperty: 'water-level',
          unit: 'cm',
          measuringPoint: { id: 'point-1', name: 'Hauptpegel' },
          station: {
            id: 'station-1',
            name: 'Wien Brigittenau',
            waterBody: 'Donau',
          },
          latestMeasurement: { observedAt: '2026-07-22T10:00:00Z', value: 312.5 },
        },
      ],
    });

    TestBed.configureTestingModule({
      imports: [TimeSeriesOverviewComponent],
      providers: [
        {
          provide: MonitoringApiService,
          useValue: { timeSeriesCollectionResource: vi.fn(() => resource) },
        },
      ],
    });
  });

  it('projects the monitoring response directly for the grid', () => {
    const fixture = createComponent();
    const grid = fixture.debugElement.query(By.directive(PhTimeSeriesGridComponent))
      .componentInstance as PhTimeSeriesGridComponent;

    expect(grid.rows()[0]).toMatchObject({
      id: 'series-w',
      measuringPointName: 'Hauptpegel',
      latestMeasurement: { valueLabel: '312,5 cm' },
    });
    expect(normalizedText(fixture)).toContain('1 Messreihe');
  });

  it('updates the result summary when grid filtering changes', () => {
    const fixture = createComponent();
    const grid = fixture.debugElement.query(By.directive(PhTimeSeriesGridComponent))
      .componentInstance as PhTimeSeriesGridComponent;

    grid.visibleCountChange.emit(0);
    fixture.detectChanges();

    expect(normalizedText(fixture)).toContain('0 von 1 Messreihen');
    expect(grid.emptyMessage()).toBe('Keine Messreihe entspricht den Filtern.');
  });
});

function createComponent(): ComponentFixture<TimeSeriesOverviewComponent> {
  const fixture = TestBed.createComponent(TimeSeriesOverviewComponent);
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
