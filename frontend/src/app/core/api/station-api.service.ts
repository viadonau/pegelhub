import { httpResource } from '@angular/common/http';
import { inject, Injectable, Injector, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import { StationDto, StationOwnerDto } from './station.dto';

@Injectable({ providedIn: 'root' })
export class StationApiService {
  private readonly apiUrl = inject(CoreApiUrlService);
  private readonly injector = inject(Injector);

  stationsResource() {
    return httpResource<StationDto[]>(() => this.apiUrl.url('/stations'), {
      defaultValue: [],
      injector: this.injector,
    });
  }

  stationResource(stationId: Signal<string>) {
    return httpResource<StationDto | null>(
      () => {
        const id = stationId().trim();

        return id ? this.apiUrl.url(`/stations/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector: this.injector,
      },
    );
  }

  stationOwnerResource(ownerId: Signal<string | null>) {
    return httpResource<StationOwnerDto | null>(
      () => {
        const id = ownerId();

        return id ? this.apiUrl.url(`/station-owners/${id}`) : undefined;
      },
      {
        defaultValue: null,
        injector: this.injector,
      },
    );
  }
}
