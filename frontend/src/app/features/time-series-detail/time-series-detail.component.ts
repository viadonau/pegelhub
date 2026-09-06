import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { MonitoringApiService } from '../../core/api/monitoring-api.service';
import { observedPropertyUnit } from '../../core/time-series/parameter-legend';
import { PhContentStateComponent } from '../../ui/content-state/content-state.component';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhCurrentReadingComponent } from './current-reading/current-reading.component';
import { latestMeasurementView } from './model/measurement-view';
import {
  measuringPointFacts,
  measurementTypeLabel,
  timeSeriesFacts,
  timeSeriesHeadingContext,
} from './model/detail-projection';
import { waterLevelChartReferences } from './model/water-level-reference';
import { PhTimeSeriesMeasurementsComponent } from './time-series-measurements/time-series-measurements.component';
import { PhTimeSeriesMetadataComponent } from './time-series-metadata/time-series-metadata.component';

@Component({
  selector: 'app-time-series-detail',
  imports: [
    PhContentStateComponent,
    PhCurrentReadingComponent,
    PhPageComponent,
    PhTimeSeriesMeasurementsComponent,
    PhTimeSeriesMetadataComponent,
    RouterLink,
  ],
  templateUrl: './time-series-detail.component.html',
  styleUrl: './time-series-detail.component.scss',
})
export class TimeSeriesDetailComponent {
  private readonly titleService = inject(Title);
  private readonly monitoringApi = inject(MonitoringApiService);
  readonly timeSeriesId = input('');
  private readonly normalizedTimeSeriesId = computed(() => this.timeSeriesId().trim());
  private readonly snapshotResource = this.monitoringApi.timeSeriesDetailResource(
    this.normalizedTimeSeriesId,
  );
  private readonly loadedSnapshotId = signal<string | null>(null);

  protected readonly snapshot = computed(() =>
    this.snapshotResource.hasValue() && !this.snapshotResource.isLoading()
      ? this.snapshotResource.value()
      : null,
  );
  protected readonly measuringPoint = computed(() => this.snapshot()?.measuringPoint ?? null);
  protected readonly station = computed(() => this.snapshot()?.station ?? null);
  protected readonly stationOwner = computed(() => this.snapshot()?.stationOwner ?? null);
  protected readonly measurementTypeLabel = computed(() => measurementTypeLabel(this.snapshot()));
  protected readonly unit = computed(() => this.snapshot()?.unit ?? '');
  protected readonly displayUnit = computed(() => {
    const snapshot = this.snapshot();
    return snapshot ? observedPropertyUnit(snapshot.observedProperty, snapshot.unit) : '';
  });
  protected readonly headingContext = computed(() => {
    const station = this.station();
    const point = this.measuringPoint();
    return station && point
      ? timeSeriesHeadingContext(station, point.name, this.measurementTypeLabel())
      : this.measurementTypeLabel();
  });
  protected readonly pointFacts = computed(() => {
    const point = this.measuringPoint();
    const owner = this.stationOwner();
    return point && owner ? measuringPointFacts(point, owner) : [];
  });
  protected readonly seriesFacts = computed(() => {
    const snapshot = this.snapshot();
    return snapshot ? timeSeriesFacts(snapshot) : [];
  });
  protected readonly referenceLines = computed(() => {
    const snapshot = this.snapshot();
    const point = this.measuringPoint();
    return snapshot?.observedProperty === 'water-level' && point
      ? waterLevelChartReferences(point)
      : [];
  });
  protected readonly latestReading = computed(() => {
    const snapshot = this.snapshot();
    return snapshot ? latestMeasurementView(snapshot.latestMeasurement, this.displayUnit()) : null;
  });
  protected readonly loading = this.snapshotResource.isLoading;
  protected readonly historyAvailable = computed(() => {
    const timeSeriesId = this.normalizedTimeSeriesId();

    return (
      this.snapshotResource.hasValue() ||
      (timeSeriesId.length > 0 && this.loadedSnapshotId() === timeSeriesId)
    );
  });
  protected readonly inactive = computed(() => this.snapshot()?.status === 'inactive');
  protected readonly error = computed(() =>
    this.snapshotResource.status() === 'error'
      ? 'Die Messreihe konnte nicht geladen werden. Bitte erneut versuchen oder den Datendienst prüfen.'
      : null,
  );

  constructor() {
    effect(() => {
      if (this.snapshotResource.hasValue()) {
        this.loadedSnapshotId.set(this.normalizedTimeSeriesId());
      }
    });

    effect(() => {
      const pointName = this.measuringPoint()?.name;
      const title = [pointName, this.measurementTypeLabel(), 'PegelHub']
        .filter(Boolean)
        .join(' · ');
      this.titleService.setTitle(title || 'Messreihe · PegelHub');
    });
  }

  protected reloadSnapshot(): void {
    this.snapshotResource.reload();
  }
}
