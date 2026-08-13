import { httpResource } from '@angular/common/http';
import { inject, Injectable, Injector, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { TimeSeriesDto } from './time-series.dto';

@Injectable({ providedIn: 'root' })
export class TimeSeriesApiService {
  private readonly apiUrl = inject(CoreApiUrlService);

  timeSeriesListResource(injector: Injector) {
    return httpResource<TimeSeriesDto[]>(() => this.apiUrl.url('/time-series'), {
      defaultValue: [],
      injector,
    });
  }

  timeSeriesResource(timeSeriesId: Signal<string>, injector: Injector) {
    return httpResource<TimeSeriesDto | null>(
      () => {
        const id = timeSeriesId().trim();

        return id ? this.apiUrl.url(`/time-series/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector,
      },
    );
  }
}
