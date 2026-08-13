import { httpResource } from '@angular/common/http';
import { inject, Injectable, Injector, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { StationDto, StationOwnerDto } from './station.dto';

@Injectable({ providedIn: 'root' })
export class StationApiService {
  private readonly apiUrl = inject(CoreApiUrlService);

  stationsResource(injector: Injector) {
    return httpResource<StationDto[]>(() => this.apiUrl.url('/stations'), {
      defaultValue: [],
      injector,
    });
  }

  stationResource(stationId: Signal<string>, injector: Injector) {
    return httpResource<StationDto | null>(
      () => {
        const id = stationId().trim();

        return id ? this.apiUrl.url(`/stations/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector,
      },
    );
  }

  stationOwnerResource(ownerId: Signal<string | null>, injector: Injector) {
    return httpResource<StationOwnerDto | null>(
      () => {
        const id = ownerId();

        return id ? this.apiUrl.url(`/station-owners/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector,
      },
    );
  }
}
