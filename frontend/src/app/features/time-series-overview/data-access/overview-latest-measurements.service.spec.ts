import { TestBed } from '@angular/core/testing';
import { lastValueFrom, of, throwError, toArray } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import { OverviewLatestMeasurementsService } from './overview-latest-measurements.service';

describe('OverviewLatestMeasurementsService', () => {
  it('starts each lookup independently and retains errors without failing the batch', async () => {
    const latestMeasurement = vi.fn((timeSeriesId: string) => {
      if (timeSeriesId === 'series-available') {
        return of({ observedAt: '2026-07-22T10:00:00Z', value: 312.5 });
      }

      if (timeSeriesId === 'series-error') {
        return throwError(() => new Error('unavailable'));
      }

      return of(null);
    });

    TestBed.configureTestingModule({
      providers: [
        OverviewLatestMeasurementsService,
        { provide: MeasurementApiService, useValue: { latestMeasurement } },
      ],
    });

    const emissions = await lastValueFrom(
      TestBed.inject(OverviewLatestMeasurementsService)
        .load(['series-available', 'series-empty', 'series-error'])
        .pipe(toArray()),
    );
    const initial = emissions[0];
    const final = emissions.at(-1);

    expect([...initial.values()].every((result) => result.status === 'loading')).toBe(true);
    expect(final?.get('series-available')?.status).toBe('available');
    expect(final?.get('series-empty')?.status).toBe('empty');
    expect(final?.get('series-error')?.status).toBe('error');
  });
});
