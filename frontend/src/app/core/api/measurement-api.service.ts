import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Injectable, Injector, Signal } from '@angular/core';
import { map, Observable } from 'rxjs';

import { CoreApiUrlService } from './core-api-url.service';
import {
  EMPTY_MEASUREMENT_BUCKET_LIST,
  EMPTY_MEASUREMENT_LIST,
  MeasurementBucketListDto,
  MeasurementListDto,
  MeasurementPointDto,
} from './measurement.dto';

@Injectable({ providedIn: 'root' })
export class MeasurementApiService {
  private readonly apiUrl = inject(CoreApiUrlService);
  private readonly http = inject(HttpClient);

  latestMeasurement(timeSeriesId: string): Observable<MeasurementPointDto | null> {
    return this.http
      .get<MeasurementListDto>(this.apiUrl.url(`/time-series/${timeSeriesId}/measurements`), {
        params: {
          last: '365d',
          order: 'desc',
          limit: '1',
        },
      })
      .pipe(map((response) => response.measurements[0] ?? null));
  }

  latestMeasurementResource(timeSeriesId: Signal<string | null>, injector: Injector) {
    return httpResource<MeasurementListDto>(
      () => {
        const id = timeSeriesId();

        if (!id) {
          return undefined;
        }

        return {
          url: this.apiUrl.url(`/time-series/${id}/measurements`),
          params: {
            last: '365d',
            order: 'desc',
            limit: '1',
          },
        };
      },
      {
        defaultValue: EMPTY_MEASUREMENT_LIST,
        injector,
      },
    );
  }

  measurementBucketsResource(
    timeSeriesId: Signal<string | null>,
    range: Signal<string>,
    injector: Injector,
  ) {
    return httpResource<MeasurementBucketListDto>(
      () => {
        const id = timeSeriesId();

        if (!id) {
          return undefined;
        }

        return {
          url: this.apiUrl.url(`/time-series/${id}/measurements/buckets`),
          params: {
            last: range(),
            maxPoints: '240',
          },
        };
      },
      {
        defaultValue: EMPTY_MEASUREMENT_BUCKET_LIST,
        injector,
      },
    );
  }
}
