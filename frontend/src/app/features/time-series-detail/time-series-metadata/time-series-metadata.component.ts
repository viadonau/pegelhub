import { Component, computed, input } from '@angular/core';

import { TimeSeriesDetailFact } from '../model/detail-projection';

@Component({
  selector: 'ph-time-series-metadata',
  host: {
    '[class.is-single-group]': 'isSingleGroup()',
  },
  templateUrl: './time-series-metadata.component.html',
  styleUrl: './time-series-metadata.component.scss',
})
export class PhTimeSeriesMetadataComponent {
  readonly measuringPointFacts = input.required<readonly TimeSeriesDetailFact[]>();
  readonly timeSeriesFacts = input.required<readonly TimeSeriesDetailFact[]>();

  protected readonly isSingleGroup = computed(
    () => this.measuringPointFacts().length > 0 !== this.timeSeriesFacts().length > 0,
  );
}
