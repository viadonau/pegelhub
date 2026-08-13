import { Component, computed, inject, input } from '@angular/core';

import { PhMessageComponent } from '../../../ui/message/message.component';
import { PhChartReferenceLine } from '../../../ui/chart/line-chart.component';
import { MeasurementChartPreferences } from '../data-access/measurement-chart-preferences.service';
import { TimeSeriesDetailState } from '../data-access/time-series-detail.state';
import { PhMeasurementChartComponent } from '../measurement-chart/measurement-chart.component';
import { PhMeasurementHistoryToolbarComponent } from '../measurement-history-toolbar/measurement-history-toolbar.component';

@Component({
  selector: 'ph-time-series-measurements',
  imports: [PhMeasurementChartComponent, PhMeasurementHistoryToolbarComponent, PhMessageComponent],
  templateUrl: './time-series-measurements.component.html',
  styleUrl: './time-series-measurements.component.scss',
})
export class PhTimeSeriesMeasurementsComponent {
  readonly referenceLines = input<readonly PhChartReferenceLine[]>([]);

  protected readonly measurements = inject(TimeSeriesDetailState);
  protected readonly preferences = inject(MeasurementChartPreferences);
  protected readonly visibleReferenceLines = computed(() =>
    this.preferences.showReferenceLevels() ? this.referenceLines() : [],
  );

  protected setRange(range: string): void {
    this.measurements.setRange(range);
  }

  protected reload(): void {
    this.measurements.reload();
  }

  protected setReferenceLevelsVisible(visible: boolean): void {
    this.preferences.setReferenceLevelsVisible(visible);
  }
}
