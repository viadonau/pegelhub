import { Component, inject } from '@angular/core';

import { PhMessageComponent } from '../../ui/message/message.component';
import { PhPageHeaderComponent } from '../../ui/page/page-header.component';
import { PhPageSectionComponent } from '../../ui/page/page-section.component';
import { PhPageComponent } from '../../ui/page/page.component';
import { OverviewLatestMeasurementsService } from './data-access/overview-latest-measurements.service';
import { TimeSeriesOverviewState } from './data-access/time-series-overview.state';
import { PhTimeSeriesGridComponent } from './time-series-grid/time-series-grid.component';

@Component({
  selector: 'app-time-series-overview',
  imports: [
    PhMessageComponent,
    PhPageComponent,
    PhPageHeaderComponent,
    PhPageSectionComponent,
    PhTimeSeriesGridComponent,
  ],
  providers: [OverviewLatestMeasurementsService, TimeSeriesOverviewState],
  templateUrl: './time-series-overview.component.html',
  styleUrl: './time-series-overview.component.scss',
})
export class TimeSeriesOverviewComponent {
  protected readonly overview = inject(TimeSeriesOverviewState);
}
