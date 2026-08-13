import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TimeSeriesOverviewView } from '../model/time-series-overview';
import { PhTimeSeriesGridComponent } from './time-series-grid.component';

describe('PhTimeSeriesGridComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PhTimeSeriesGridComponent],
      providers: [provideRouter([])],
    });
  });

  it('renders one native row per time series with the shared cell hierarchy', async () => {
    const fixture = TestBed.createComponent(PhTimeSeriesGridComponent);
    fixture.componentRef.setInput('rows', [
      overviewRow('series-w', 'Wasserstand', 'Hauptpegel'),
      overviewRow('series-q', 'Abfluss', 'Durchflussmesser'),
    ]);
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(renderedCellText(fixture.nativeElement, 'series')).toEqual([
        'Hauptpegel',
        'Durchflussmesser',
      ]);
      expect(renderedCellText(fixture.nativeElement, 'measurementType')).toEqual([
        'Wasserstand',
        'Abfluss',
      ]);
      expect(
        fixture.nativeElement
          .querySelector('.ag-row [col-id="series"]')
          ?.classList.contains('ph-data-grid-cell-primary'),
      ).toBe(true);
      expect(
        fixture.nativeElement
          .querySelector('.ag-row [col-id="measurementType"]')
          ?.classList.contains('ph-data-grid-cell-secondary'),
      ).toBe(true);
      expect(
        fixture.nativeElement
          .querySelector('.ag-row [col-id="latestValue"]')
          ?.classList.contains('ph-data-grid-cell-emphasis'),
      ).toBe(true);
    });
  });

  it('refreshes a row when its latest measurement resolves', async () => {
    const fixture = TestBed.createComponent(PhTimeSeriesGridComponent);
    const initial = overviewRow('series-w', 'Wasserstand', 'Hauptpegel');
    const resolved: TimeSeriesOverviewView = {
      ...initial,
      latestMeasurement: {
        value: 278,
        valueLabel: '278 cm',
        observedAt: '2026-07-22T10:00:00Z',
        timestamp: '22. Juli 2026 um 12:00',
        activityLabel: 'vor 4 Min.',
      },
    };

    fixture.componentRef.setInput('rows', [initial]);
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(renderedCellText(fixture.nativeElement, 'latestValue')).toEqual(['Wird geladen']);
    });

    fixture.componentRef.setInput('rows', [resolved]);
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(renderedCellText(fixture.nativeElement, 'latestValue')).toEqual(['278 cm']);
      expect(renderedCellText(fixture.nativeElement, 'activity')).toEqual(['vor 4 Min.']);
    });
  });

  it('only opens detail from the chevron action', async () => {
    const fixture = TestBed.createComponent(PhTimeSeriesGridComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentRef.setInput('rows', [overviewRow('series-w', 'Wasserstand', 'Hauptpegel')]);
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(fixture.nativeElement.querySelector('.ag-row [col-id="open"] button')).not.toBeNull();
    });

    fixture.nativeElement.querySelector('.ag-row [col-id="series"]').click();
    expect(navigate).not.toHaveBeenCalled();

    fixture.nativeElement.querySelector('.ag-row [col-id="open"] button').click();
    expect(navigate).toHaveBeenCalledWith(['/overview', 'series-w']);
  });
});

function overviewRow(
  id: string,
  measurementTypeLabel: string,
  measuringPointName: string,
): TimeSeriesOverviewView {
  return {
    id,
    measurementTypeLabel,
    measuringPointName,
    stationLabel: 'Wien Brigittenau · 10001030 · Donau',
    latestMeasurement: {
      value: null,
      valueLabel: 'Wird geladen',
      observedAt: null,
      timestamp: null,
      activityLabel: 'Wird geladen',
    },
  };
}

function renderedCellText(root: HTMLElement, columnId: string): string[] {
  return Array.from(root.querySelectorAll<HTMLElement>(`.ag-row [col-id="${columnId}"]`)).map(
    (element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '',
  );
}
