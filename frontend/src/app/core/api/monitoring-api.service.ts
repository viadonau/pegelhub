import { httpResource } from '@angular/common/http';
import { inject, Injectable, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { MonitoringTimeSeriesCollectionDto, MonitoringTimeSeriesDetailDto } from './monitoring.dto';

@Injectable({ providedIn: 'root' })
export class MonitoringApiService {
  private readonly apiUrl = inject(CoreApiUrlService);

  timeSeriesCollectionResource() {
    return httpResource<MonitoringTimeSeriesCollectionDto>(() => ({
      url: this.apiUrl.url('/monitoring/time-series'),
      params: { latestWithin: '365d' },
    }));
  }

  timeSeriesDetailResource(timeSeriesId: Signal<string>) {
    return httpResource<MonitoringTimeSeriesDetailDto>(() => {
      const id = timeSeriesId().trim();

      return id
        ? {
            url: this.apiUrl.url(`/monitoring/time-series/${id}`),
            params: { latestWithin: '365d' },
          }
        : undefined;
    });
  }
}
