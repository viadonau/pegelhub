import { Component, computed, inject, input, output, signal } from '@angular/core';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import { observedPropertyLabel } from '../../../core/time-series/parameter-legend';
import { PhMessageComponent } from '../../../ui/message/message.component';
import { PhChartReferenceLine } from '../../../ui/chart/line-chart.component';
import { MeasurementChartPreferences } from '../data-access/measurement-chart-preferences.service';
import { measurementChartSeries } from '../model/measurement-view';
import { PhMeasurementChartComponent } from '../measurement-chart/measurement-chart.component';
import { PhMeasurementHistoryToolbarComponent } from '../measurement-history-toolbar/measurement-history-toolbar.component';

@Component({
  selector: 'ph-time-series-measurements',
  imports: [PhMeasurementChartComponent, PhMeasurementHistoryToolbarComponent, PhMessageComponent],
  templateUrl: './time-series-measurements.component.html',
  styleUrl: './time-series-measurements.component.scss',
})
export class PhTimeSeriesMeasurementsComponent {
  readonly timeSeriesId = input('');
  readonly observedProperty = input('');
  readonly unit = input('');
  readonly referenceLines = input<readonly PhChartReferenceLine[]>([]);
  readonly refresh = output<void>();

  protected readonly preferences = inject(MeasurementChartPreferences);
  protected readonly visibleReferenceLines = computed(() =>
    this.preferences.showReferenceLevels() ? this.referenceLines() : [],
  );
  protected readonly selectedRange = signal('24h');
  private readonly normalizedTimeSeriesId = computed(() => this.timeSeriesId().trim());
  private readonly measurementApi = inject(MeasurementApiService);
  private readonly bucketsResource = this.measurementApi.measurementBucketsResource(
    this.normalizedTimeSeriesId,
    this.selectedRange,
  );

  protected readonly chartSeries = computed(() => {
    if (this.bucketsResource.hasValue() && !this.bucketsResource.isLoading()) {
      const response = this.bucketsResource.value();
      return measurementChartSeries(response, this.observedProperty());
    }

    return null;
  });
  protected readonly chartParameter = computed(() =>
    observedPropertyLabel(this.observedProperty()),
  );
  protected readonly chartUnit = computed(() => this.unit().trim() || null);
  protected readonly chartYLabel = computed(() => {
    const parameter = this.chartParameter();
    const unit = this.chartUnit();
    return unit ? `${parameter} (${unit})` : parameter;
  });
  protected readonly chartTitle = computed(() => {
    const label = this.chartYLabel();
    return label ? `Messverlauf · ${label}` : 'Messverlauf';
  });
  protected readonly historyLoading = this.bucketsResource.isLoading;
  protected readonly historyError = computed(() =>
    this.bucketsResource.status() === 'error'
      ? 'Der Messverlauf konnte nicht geladen werden.'
      : null,
  );
  protected readonly emptyMessage = computed(() => {
    const parameter = this.chartParameter();
    return parameter
      ? `Für ${parameter} sind in diesem Zeitraum keine Messwerte vorhanden.`
      : 'Für diesen Zeitraum sind keine Messwerte vorhanden.';
  });

  protected setRange(range: string): void {
    this.selectedRange.set(range);
  }

  protected reload(): void {
    this.bucketsResource.reload();
    this.refresh.emit();
  }
}
