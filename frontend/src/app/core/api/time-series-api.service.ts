import { httpResource } from '@angular/common/http';
import { inject, Injectable, Injector, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { TimeSeriesDto } from './time-series.dto';

@Injectable({ providedIn: 'root' })
export class TimeSeriesApiService {
  private readonly apiUrl = inject(CoreApiUrlService);
  private readonly injector = inject(Injector);

  timeSeriesListResource() {
    return httpResource<TimeSeriesDto[]>(() => this.apiUrl.url('/time-series'), {
      defaultValue: [],
      injector: this.injector,
    });
  }

  timeSeriesResource(timeSeriesId: Signal<string>) {
    return httpResource<TimeSeriesDto | null>(
      () => {
        const id = timeSeriesId().trim();

        return id ? this.apiUrl.url(`/time-series/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector: this.injector,
      },
    );
  }
}
