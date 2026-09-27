import { Component, input } from '@angular/core';

import { TimeSeriesDetailFact } from '../model/detail-projection';

@Component({
  selector: 'ph-time-series-metadata',
  templateUrl: './time-series-metadata.component.html',
  styleUrl: './time-series-metadata.component.scss',
})
export class PhTimeSeriesMetadataComponent {
  readonly measuringPointFacts = input.required<readonly TimeSeriesDetailFact[]>();
  readonly timeSeriesFacts = input.required<readonly TimeSeriesDetailFact[]>();
}
