import {
  computed,
  inject,
  Injectable,
  Injector,
  linkedSignal,
  Signal,
  signal,
  WritableSignal,
} from '@angular/core';

import { MeasuringPointApiService } from '../../../core/api/measuring-point-api.service';
import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import {
  EMPTY_MEASUREMENT_BUCKET_LIST,
  EMPTY_MEASUREMENT_LIST,
  MeasurementBucketListDto,
  MeasurementListDto,
} from '../../../core/api/measurement.dto';
import { StationApiService } from '../../../core/api/station-api.service';
import { TimeSeriesApiService } from '../../../core/api/time-series-api.service';
import { observedPropertyLabel } from '../../../core/time-series/parameter-legend';
import { latestMeasurementView, measurementChartSeries } from '../model/measurement-view';
import { timeSeriesDetailView } from '../model/time-series-detail-view';

/** Route-scoped state for the detail resources, projections, and commands. */
@Injectable()
export class TimeSeriesDetailState {
  private readonly resourceInjector = inject(Injector);
  private readonly measuringPointApi = inject(MeasuringPointApiService);
  private readonly measurementsApi = inject(MeasurementApiService);
  private readonly stationApi = inject(StationApiService);
  private readonly timeSeriesApi = inject(TimeSeriesApiService);
  private connected = false;

  private timeSeriesResourceRef?: ReturnType<TimeSeriesApiService['timeSeriesResource']>;
  private measuringPointResourceRef?: ReturnType<
    MeasuringPointApiService['measuringPointResource']
  >;
  private stationResourceRef?: ReturnType<StationApiService['stationResource']>;
  private stationOwnerResourceRef?: ReturnType<StationApiService['stationOwnerResource']>;
  private latestMeasurements?: ReturnType<MeasurementApiService['latestMeasurementResource']>;
  private measurementBuckets?: ReturnType<MeasurementApiService['measurementBucketsResource']>;
  private displayedLatest?: WritableSignal<MeasurementListDto>;
  private displayedBuckets?: WritableSignal<MeasurementBucketListDto>;

  readonly selectedRange = signal('24h');
  readonly timeSeries = computed(() => {
    const resource = this.timeSeriesResourceRef;

    return resource?.hasValue() ? resource.value() : null;
  });
  readonly measuringPoint = computed(() => {
    const resource = this.measuringPointResourceRef;

    return resource?.hasValue() ? resource.value() : null;
  });
  readonly station = computed(() => {
    const resource = this.stationResourceRef;

    return resource?.hasValue() ? resource.value() : null;
  });
  readonly stationOwner = computed(() => {
    const resource = this.stationOwnerResourceRef;

    return resource?.hasValue() ? (resource.value() ?? undefined) : undefined;
  });
  readonly view = computed(() =>
    timeSeriesDetailView(
      this.timeSeries(),
      this.measuringPoint(),
      this.station(),
      this.stationOwner(),
    ),
  );

  readonly parameterLabel = computed(() => {
    const timeSeries = this.timeSeries();

    return timeSeries ? observedPropertyLabel(timeSeries.observedProperty) : '';
  });
  readonly unit = computed(() => this.timeSeries()?.unit || null);
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
    const response = this.displayedLatest?.();
    const timeSeries = this.timeSeries();

    return response && timeSeries && response.timeSeriesId === timeSeries.id
      ? latestMeasurementView(response, timeSeries)
      : null;
  });
  readonly chartSeries = computed(() => {
    const response = this.displayedBuckets?.();
    const timeSeries = this.timeSeries();

    return response && timeSeries && response.timeSeriesId === timeSeries.id
      ? measurementChartSeries(response, timeSeries)
      : null;
  });
  readonly latestLoading = computed(() => this.latestMeasurements?.isLoading() ?? false);
  readonly latestError = computed(() => this.latestMeasurements?.status() === 'error');
  readonly historyLoading = computed(() => this.measurementBuckets?.isLoading() ?? false);
  readonly timeSeriesLoading = computed(() => this.timeSeriesResourceRef?.isLoading() ?? false);
  readonly pointLoading = computed(() => this.measuringPointResourceRef?.isLoading() ?? false);
  readonly historyError = computed(() =>
    this.measurementBuckets?.status() === 'error'
      ? 'Der Messverlauf konnte nicht geladen werden.'
      : null,
  );
  readonly emptyMessage = computed(
    () => `Für ${this.parameterLabel()} sind in diesem Zeitraum keine Messwerte vorhanden.`,
  );

  readonly timeSeriesError = computed(() =>
    this.timeSeriesResourceRef?.status() === 'error'
      ? 'Die Messreihe konnte nicht geladen werden. Bitte erneut versuchen oder den Datendienst prüfen.'
      : null,
  );
  readonly pointError = computed(() =>
    this.measuringPointResourceRef?.status() === 'error'
      ? 'Der zugeordnete Messpunkt konnte nicht geladen werden.'
      : null,
  );
  readonly stationError = computed(() =>
    this.stationResourceRef?.status() === 'error'
      ? 'Die zugeordnete Pegelstelle konnte nicht geladen werden.'
      : null,
  );
  readonly ownerError = computed(() =>
    this.stationOwnerResourceRef?.status() === 'error'
      ? 'Die zugeordnete Organisation konnte nicht geladen werden.'
      : null,
  );

  /** Connects the route input once; resources then react directly to that signal. */
  connect(timeSeriesId: Signal<string>): void {
    if (this.connected) {
      return;
    }

    this.connected = true;
    const routeTimeSeriesId = computed(() => timeSeriesId().trim());
    const measuringPointId = computed(() => this.timeSeries()?.measuringPointId ?? '');
    const stationId = computed(() => this.measuringPoint()?.stationId ?? '');
    const ownerId = computed(() => this.station()?.ownerId ?? null);
    const activeTimeSeriesId = computed(() => this.timeSeries()?.id ?? null);

    this.timeSeriesResourceRef = this.timeSeriesApi.timeSeriesResource(
      routeTimeSeriesId,
      this.resourceInjector,
    );
    this.measuringPointResourceRef = this.measuringPointApi.measuringPointResource(
      measuringPointId,
      this.resourceInjector,
    );
    this.stationResourceRef = this.stationApi.stationResource(stationId, this.resourceInjector);
    this.stationOwnerResourceRef = this.stationApi.stationOwnerResource(
      ownerId,
      this.resourceInjector,
    );
    this.latestMeasurements = this.measurementsApi.latestMeasurementResource(
      activeTimeSeriesId,
      this.resourceInjector,
    );
    this.measurementBuckets = this.measurementsApi.measurementBucketsResource(
      activeTimeSeriesId,
      this.selectedRange,
      this.resourceInjector,
    );

    this.displayedLatest = linkedSignal({
      source: () => ({
        activeId: activeTimeSeriesId(),
        response: this.latestMeasurements!.value(),
      }),
      computation: (source, previous) =>
        source.response.timeSeriesId === source.activeId
          ? source.response
          : (previous?.value ?? EMPTY_MEASUREMENT_LIST),
    });
    this.displayedBuckets = linkedSignal({
      source: () => ({
        activeId: activeTimeSeriesId(),
        response: this.measurementBuckets!.value(),
      }),
      computation: (source, previous) =>
        source.response.timeSeriesId === source.activeId
          ? source.response
          : (previous?.value ?? EMPTY_MEASUREMENT_BUCKET_LIST),
    });
  }

  setRange(range: string): void {
    if (range !== this.selectedRange()) {
      this.selectedRange.set(range);
    }
  }

  reload(): void {
    this.latestMeasurements?.reload();
    this.measurementBuckets?.reload();
  }
}
