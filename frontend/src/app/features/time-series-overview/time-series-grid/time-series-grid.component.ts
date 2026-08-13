import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { PhDataGridComponent } from '../../../ui/data-grid/data-grid';
import { TimeSeriesOverviewView } from '../model/time-series-overview';
import { TIME_SERIES_GRID_VISIBLE_FROM, timeSeriesGridColumns } from './time-series-grid.columns';

@Component({
  selector: 'ph-time-series-grid',
  imports: [PhDataGridComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './time-series-grid.component.html',
})
export class PhTimeSeriesGridComponent {
  readonly rows = input.required<readonly TimeSeriesOverviewView[]>();
  readonly emptyMessage = input('Keine Messreihen vorhanden.');
  readonly visibleCountChange = output<number>();

  protected readonly columnDefs = timeSeriesGridColumns();
  protected readonly visibleFrom = TIME_SERIES_GRID_VISIBLE_FROM;
}
