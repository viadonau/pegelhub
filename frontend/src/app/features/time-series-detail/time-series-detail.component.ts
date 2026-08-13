import { Component, effect, inject, input } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { PhContentStateComponent } from '../../ui/content-state/content-state.component';
import { PhMessageComponent } from '../../ui/message/message.component';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhCurrentReadingComponent } from './current-reading/current-reading.component';
import { TimeSeriesDetailState } from './data-access/time-series-detail.state';
import { PhTimeSeriesMeasurementsComponent } from './time-series-measurements/time-series-measurements.component';
import { PhTimeSeriesMetadataComponent } from './time-series-metadata/time-series-metadata.component';

@Component({
  selector: 'app-time-series-detail',
  imports: [
    PhContentStateComponent,
    PhCurrentReadingComponent,
    PhMessageComponent,
    PhPageComponent,
    PhTimeSeriesMeasurementsComponent,
    PhTimeSeriesMetadataComponent,
    RouterLink,
  ],
  providers: [TimeSeriesDetailState],
  templateUrl: './time-series-detail.component.html',
  styleUrl: './time-series-detail.component.scss',
})
export class TimeSeriesDetailComponent {
  readonly timeSeriesId = input('');

  private readonly titleService = inject(Title);
  protected readonly detail = inject(TimeSeriesDetailState);

  constructor() {
    this.detail.connect(this.timeSeriesId);

    effect(() => {
      const view = this.detail.view();
      const pointName = view.measuringPoint?.name;
      const measurementTypeLabel = view.measurementTypeLabel;
      const title = [pointName, measurementTypeLabel, 'PegelHub'].filter(Boolean).join(' · ');

      this.titleService.setTitle(title || 'Messreihe · PegelHub');
    });
  }
}
