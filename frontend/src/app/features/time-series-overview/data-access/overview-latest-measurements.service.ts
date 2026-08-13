import { inject, Injectable } from '@angular/core';
import { catchError, from, map, mergeMap, Observable, of, scan, startWith } from 'rxjs';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import { LatestMeasurementLookup, LatestMeasurementResult } from '../model/latest-measurement';

const MAX_CONCURRENT_REQUESTS = 6;

@Injectable()
export class OverviewLatestMeasurementsService {
  private readonly measurements = inject(MeasurementApiService);

  load(timeSeriesIds: readonly string[]): Observable<LatestMeasurementLookup> {
    const uniqueIds = [...new Set(timeSeriesIds)];

    if (uniqueIds.length === 0) {
      return of(new Map());
    }

    const initialLookup: LatestMeasurementLookup = new Map(
      uniqueIds.map((timeSeriesId) => [timeSeriesId, { status: 'loading' }]),
    );

    return from(uniqueIds).pipe(
      mergeMap((timeSeriesId) => this.loadOne(timeSeriesId), MAX_CONCURRENT_REQUESTS),
      scan((lookup, [timeSeriesId, result]): LatestMeasurementLookup => {
        const nextLookup = new Map(lookup);
        nextLookup.set(timeSeriesId, result);

        return nextLookup;
      }, initialLookup),
      startWith(initialLookup),
    );
  }

  private loadOne(timeSeriesId: string): Observable<readonly [string, LatestMeasurementResult]> {
    return this.measurements.latestMeasurement(timeSeriesId).pipe(
      map((measurement): readonly [string, LatestMeasurementResult] => [
        timeSeriesId,
        measurement ? { status: 'available', measurement } : { status: 'empty' },
      ]),
      catchError(() =>
        of<readonly [string, LatestMeasurementResult]>([timeSeriesId, { status: 'error' }]),
      ),
    );
  }
}
