import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import {
  EMPTY_MEASUREMENT_BUCKET_LIST,
  EMPTY_MEASUREMENT_LIST,
  MeasurementBucketListDto,
  MeasurementListDto,
} from '../../../core/api/measurement.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import { observedPropertyLabel } from '../../../core/time-series/parameter-legend';
import { latestMeasurementView, measurementChartSeries } from '../model/measurement-view';

@Injectable()
export class TimeSeriesDetailMeasurementsStore {
  private readonly measurementsApi = inject(MeasurementApiService);
  private readonly activeTimeSeries = signal<TimeSeriesDto | null>(null);
  private readonly timeSeriesId = computed(() => this.activeTimeSeries()?.id ?? null);
  private readonly displayedLatest = signal<MeasurementListDto>(EMPTY_MEASUREMENT_LIST);
  private readonly displayedBuckets = signal<MeasurementBucketListDto>(
    EMPTY_MEASUREMENT_BUCKET_LIST,
  );

  readonly selectedRange = signal('24h');
  readonly latestMeasurements = this.measurementsApi.latestMeasurementResource(this.timeSeriesId);
  readonly measurementBuckets = this.measurementsApi.measurementBucketsResource(
    this.timeSeriesId,
    this.selectedRange,
  );
  readonly parameterLabel = computed(() => {
    const timeSeries = this.activeTimeSeries();

    return timeSeries ? observedPropertyLabel(timeSeries.observedProperty) : '';
  });
  readonly unit = computed(() => this.activeTimeSeries()?.unit || null);
  readonly chartYLabel = computed(() => {
    const unit = this.unit();

    return unit ? `${this.parameterLabel()} (${unit})` : this.parameterLabel();
  });
  readonly chartTitle = computed(() => {
    const parameter = this.parameterLabel();
    const unit = this.unit();

    return unit ? `Messverlauf · ${parameter} (${unit})` : `Messverlauf · ${parameter}`;
  });
  readonly latestReading = computed(() => {
    const response = this.displayedLatest();
    const timeSeries = this.activeTimeSeries();

    return timeSeries && response.timeSeriesId === timeSeries.id
      ? latestMeasurementView(response, timeSeries)
      : null;
  });
  readonly chartSeries = computed(() => {
    const response = this.displayedBuckets();
    const timeSeries = this.activeTimeSeries();

    return timeSeries && response.timeSeriesId === timeSeries.id
      ? measurementChartSeries(response, timeSeries)
      : null;
  });
  readonly latestLoading = computed(() => this.latestMeasurements.isLoading());
  readonly latestError = computed(() => this.latestMeasurements.status() === 'error');
  readonly historyLoading = computed(() => this.measurementBuckets.isLoading());
  readonly historyError = computed(() =>
    this.measurementBuckets.status() === 'error'
      ? 'Der Messverlauf konnte nicht geladen werden.'
      : null,
  );
  readonly emptyMessage = computed(
    () => `Für ${this.parameterLabel()} sind in diesem Zeitraum keine Messwerte vorhanden.`,
  );

  constructor() {
    effect(() => {
      this.retainResolvedResponse(this.latestMeasurements, this.displayedLatest);
    });

    effect(() => {
      this.retainResolvedResponse(this.measurementBuckets, this.displayedBuckets);
    });
  }

  setTimeSeries(timeSeries: TimeSeriesDto | null): void {
    this.activeTimeSeries.set(timeSeries);
  }

  setRange(range: string): void {
    if (range !== this.selectedRange()) {
      this.selectedRange.set(range);
    }
  }

  reload(): void {
    this.latestMeasurements.reload();
    this.measurementBuckets.reload();
  }

  private retainResolvedResponse<T extends { timeSeriesId: string }>(
    resource: {
      status(): string;
      value(): T;
    },
    target: { set(value: T): void },
  ): void {
    const activeId = this.timeSeriesId();

    if (resource.status() !== 'resolved') {
      return;
    }

    const response = resource.value();

    if (response.timeSeriesId === activeId) {
      target.set(response);
    }
  }
}
