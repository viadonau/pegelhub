import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { computed, inject, Injectable, Injector, signal } from '@angular/core';
import { distinctUntilChanged, switchMap } from 'rxjs';

import { MeasuringPointApiService } from '../../../core/api/measuring-point-api.service';
import { StationApiService } from '../../../core/api/station-api.service';
import { TimeSeriesApiService } from '../../../core/api/time-series-api.service';
import { OverviewLatestMeasurementsService } from './overview-latest-measurements.service';
import { LatestMeasurementLookup } from '../model/latest-measurement';
import { timeSeriesOverviewViews } from '../model/time-series-overview';

const EMPTY_LATEST_MEASUREMENTS: LatestMeasurementLookup = new Map();

/** Route-scoped read model for the complete overview workflow. */
@Injectable()
export class TimeSeriesOverviewState {
  private readonly resourceInjector = inject(Injector);
  private readonly measuringPointApi = inject(MeasuringPointApiService);
  private readonly stationApi = inject(StationApiService);
  private readonly timeSeriesApi = inject(TimeSeriesApiService);
  private readonly latestMeasurementLoader = inject(OverviewLatestMeasurementsService);

  private readonly measuringPoints = this.measuringPointApi.measuringPointsResource(
    this.resourceInjector,
  );
  private readonly stations = this.stationApi.stationsResource(this.resourceInjector);
  private readonly timeSeries = this.timeSeriesApi.timeSeriesListResource(this.resourceInjector);
  readonly visibleRowCount = signal<number | null>(null);

  private readonly resolvedMeasuringPoints = computed(() =>
    this.measuringPoints.hasValue() ? this.measuringPoints.value() : [],
  );
  private readonly resolvedStations = computed(() =>
    this.stations.hasValue() ? this.stations.value() : [],
  );
  private readonly resolvedTimeSeries = computed(() =>
    this.timeSeries.hasValue() ? this.timeSeries.value() : [],
  );
  private readonly timeSeriesIds = computed(() =>
    this.resolvedTimeSeries()
      .map((item) => item.id)
      .sort(),
  );

  private readonly latestMeasurements = toSignal(
    toObservable(this.timeSeriesIds).pipe(
      distinctUntilChanged(sameIds),
      switchMap((timeSeriesIds) => this.latestMeasurementLoader.load(timeSeriesIds)),
    ),
    { initialValue: EMPTY_LATEST_MEASUREMENTS },
  );
  readonly rows = computed(() =>
    timeSeriesOverviewViews(
      this.resolvedMeasuringPoints(),
      this.resolvedStations(),
      this.resolvedTimeSeries(),
      this.latestMeasurements(),
    ),
  );
  readonly resultSummary = computed(() => {
    const seriesCount = this.rows().length;
    const visibleCount = this.visibleRowCount() ?? seriesCount;

    if (visibleCount !== seriesCount) {
      return `${visibleCount} von ${seriesCount} Messreihen`;
    }

    return seriesCount === 1 ? '1 Messreihe' : `${seriesCount} Messreihen`;
  });
  readonly emptyMessage = computed(() => {
    return this.rows().length > 0 && this.visibleRowCount() === 0
      ? 'Keine Messreihe entspricht den Filtern.'
      : 'Keine Messreihen vorhanden.';
  });
  readonly errorMessage = computed(() =>
    this.measuringPoints.status() === 'error'
      ? 'Messpunkte konnten nicht geladen werden. Die Messreihenübersicht ist deshalb nicht verfügbar.'
      : null,
  );
  readonly supportingError = computed(() => {
    if (this.stations.status() === 'error' && this.timeSeries.status() === 'error') {
      return 'Pegelstellen und Messreihen konnten nicht geladen werden.';
    }

    if (this.stations.status() === 'error') {
      return 'Die Pegelstellenzuordnung konnte nicht vollständig geladen werden.';
    }

    return this.timeSeries.status() === 'error' ? 'Messreihen konnten nicht geladen werden.' : null;
  });
  readonly isInitialLoading = computed(
    () =>
      this.measuringPoints.isLoading() || this.stations.isLoading() || this.timeSeries.isLoading(),
  );

  updateVisibleRowCount(count: number): void {
    this.visibleRowCount.set(count);
  }
}

function sameIds(left: readonly string[], right: readonly string[]): boolean {
  return left.length === right.length && left.every((id, index) => id === right[index]);
}
