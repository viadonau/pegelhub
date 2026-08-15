import { httpResource } from '@angular/common/http';
import { inject, Injectable, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { MeasurementBucketListDto } from './measurement.dto';

@Injectable({ providedIn: 'root' })
export class MeasurementApiService {
  private readonly apiUrl = inject(CoreApiUrlService);

  measurementBucketsResource(timeSeriesId: Signal<string>, range: Signal<string>) {
    return httpResource<MeasurementBucketListDto>(() => {
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
    });
  }
}
