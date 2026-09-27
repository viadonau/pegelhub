import { httpResource } from '@angular/common/http';
import { inject, Injectable, Signal } from '@angular/core';

import { CoreApiUrlService } from './core-api-url.service';
import {
  MeasurementIntervalListDto,
  MeasurementIntervalRequest,
  MeasurementRawRequest,
  RawMeasurementListDto,
} from './measurement.dto';

@Injectable({ providedIn: 'root' })
export class MeasurementApiService {
  private readonly apiUrl = inject(CoreApiUrlService);

  measurementIntervalsResource(
    timeSeriesId: Signal<string>,
    query: Signal<MeasurementIntervalRequest | null>,
  ) {
    return httpResource<MeasurementIntervalListDto>(() => {
      const id = timeSeriesId();
      const selected = query();
      if (!id || !selected) return undefined;

      return {
        url: this.apiUrl.url(`/time-series/${encodeURIComponent(id)}/measurements/intervals`),
        params: {
          from: selected.from,
          to: selected.to,
          interval: selected.interval,
          timeBasis: selected.timeBasis,
          closedOnly: 'true',
          representation: 'canonical',
        },
      };
    });
  }

  rawMeasurementsResource(
    timeSeriesId: Signal<string>,
    query: Signal<MeasurementRawRequest | null>,
  ) {
    return httpResource<RawMeasurementListDto>(() => {
      const id = timeSeriesId();
      const selected = query();
      if (!id || !selected) return undefined;

      return {
        url: this.apiUrl.url(`/time-series/${encodeURIComponent(id)}/measurements`),
        params: {
          from: selected.from,
          to: selected.to,
          order: 'asc',
          limit: '1000',
          representation: 'canonical',
        },
      };
    });
  }
}
