import { httpResource } from '@angular/common/http';
import { inject, Injectable, Injector, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { MeasuringPointDto } from './measuring-point.dto';

@Injectable({ providedIn: 'root' })
export class MeasuringPointApiService {
  private readonly apiUrl = inject(CoreApiUrlService);
  private readonly injector = inject(Injector);

  measuringPointsResource() {
    return httpResource<MeasuringPointDto[]>(() => this.apiUrl.url('/measuring-points'), {
      defaultValue: [],
      injector: this.injector,
    });
  }

  measuringPointResource(measuringPointId: Signal<string>) {
    return httpResource<MeasuringPointDto | null>(
      () => {
        const id = measuringPointId().trim();

        return id ? this.apiUrl.url(`/measuring-points/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector: this.injector,
      },
    );
  }
}
