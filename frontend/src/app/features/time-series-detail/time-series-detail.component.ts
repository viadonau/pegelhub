import { Component, computed, effect, inject, input } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { MonitoringApiService } from '../../core/api/monitoring-api.service';
import { PhContentStateComponent } from '../../ui/content-state/content-state.component';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhCurrentReadingComponent } from './current-reading/current-reading.component';
import { latestMeasurementView } from './model/measurement-view';
import { timeSeriesDetailView } from './model/time-series-detail-view';
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

  private readonly snapshot = computed(() => {
    if (!this.snapshotResource.hasValue() || this.snapshotResource.isLoading()) {
      return null;
    }

    return this.snapshotResource.value();
  });

  protected readonly detail = computed(() => timeSeriesDetailView(this.snapshot()));
  protected readonly latestReading = computed(() => {
    const snapshot = this.snapshot();
    return snapshot ? latestMeasurementView(snapshot.latestMeasurement, snapshot.unit) : null;
  });
  protected readonly loading = this.snapshotResource.isLoading;
  protected readonly error = computed(() =>
    this.snapshotResource.status() === 'error'
      ? 'Die Messreihe konnte nicht geladen werden. Bitte erneut versuchen oder den Datendienst prüfen.'
      : null,
  );

  constructor() {
    effect(() => {
      const view = this.detail();
      const pointName = view.measuringPoint?.name;
      const measurementTypeLabel = view.measurementTypeLabel;
      const title = [pointName, measurementTypeLabel, 'PegelHub'].filter(Boolean).join(' · ');

      this.titleService.setTitle(title || 'Messreihe · PegelHub');
    });
  }

  protected reloadSnapshot(): void {
    this.snapshotResource.reload();
  }
}
