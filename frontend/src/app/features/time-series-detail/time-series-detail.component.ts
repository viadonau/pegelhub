import { Component, computed, effect, inject, input } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { MeasuringPointApiService } from '../../core/api/measuring-point-api.service';
import { StationApiService } from '../../core/api/station-api.service';
import { TimeSeriesApiService } from '../../core/api/time-series-api.service';
import { observedPropertyLabel } from '../../core/time-series/parameter-legend';
import { PhContentStateComponent } from '../../ui/content-state/content-state.component';
import { PhMessageComponent } from '../../ui/message/message.component';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhCurrentReadingComponent } from './current-reading/current-reading.component';
import { TimeSeriesDetailMeasurementsStore } from './data-access/time-series-detail-measurements.store';
import {
  measuringPointFacts,
  referenceValueUnit,
  timeSeriesContextFacts,
  timeSeriesHeadingContext,
} from './model/time-series-detail-view';
import { waterLevelChartReferences } from './model/water-level-reference';
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
  providers: [TimeSeriesDetailMeasurementsStore],
  templateUrl: './time-series-detail.component.html',
  styleUrl: './time-series-detail.component.scss',
})
export class TimeSeriesDetailComponent {
  readonly timeSeriesId = input('');

  private readonly measuringPointApi = inject(MeasuringPointApiService);
  private readonly stationApi = inject(StationApiService);
  private readonly timeSeriesApi = inject(TimeSeriesApiService);
  private readonly titleService = inject(Title);
  protected readonly measurements = inject(TimeSeriesDetailMeasurementsStore);

  protected readonly timeSeriesResource = this.timeSeriesApi.timeSeriesResource(this.timeSeriesId);
  protected readonly timeSeries = computed(() =>
    this.timeSeriesResource.hasValue() ? this.timeSeriesResource.value() : null,
  );
  protected readonly measuringPointResource = this.measuringPointApi.measuringPointResource(
    computed(() => this.timeSeries()?.measuringPointId ?? ''),
  );
  protected readonly measuringPoint = computed(() =>
    this.measuringPointResource.hasValue() ? this.measuringPointResource.value() : null,
  );
  protected readonly stationResource = this.stationApi.stationResource(
    computed(() => this.measuringPoint()?.stationId ?? ''),
  );
  protected readonly station = computed(() =>
    this.stationResource.hasValue() ? this.stationResource.value() : null,
  );
  protected readonly stationOwnerResource = this.stationApi.stationOwnerResource(
    computed(() => this.station()?.ownerId ?? null),
  );
  protected readonly stationOwner = computed(() =>
    this.stationOwnerResource.hasValue()
      ? (this.stationOwnerResource.value() ?? undefined)
      : undefined,
  );
  private readonly referenceUnit = computed(() => referenceValueUnit(this.timeSeries()));

  protected readonly measurementTypeLabel = computed(() => {
    const timeSeries = this.timeSeries();

    return timeSeries ? observedPropertyLabel(timeSeries.observedProperty) : '';
  });
  protected readonly headingContext = computed(() => {
    const station = this.station();
    const point = this.measuringPoint();

    if (!point) {
      return '';
    }

    const measurementTypeLabel = this.measurementTypeLabel();

    return station
      ? timeSeriesHeadingContext(station, point.name, measurementTypeLabel)
      : measurementTypeLabel;
  });
  protected readonly pointFacts = computed(() => {
    const point = this.measuringPoint();

    return point ? measuringPointFacts(point, this.stationOwner(), this.referenceUnit()) : [];
  });
  protected readonly referenceLines = computed(() => {
    const point = this.measuringPoint();
    const timeSeries = this.timeSeries();

    return point && timeSeries ? waterLevelChartReferences(point, timeSeries) : [];
  });
  protected readonly seriesFacts = computed(() => {
    const timeSeries = this.timeSeries();

    return timeSeries ? timeSeriesContextFacts(timeSeries) : [];
  });
  protected readonly timeSeriesError = computed(() =>
    this.timeSeriesResource.status() === 'error'
      ? 'Die Messreihe konnte nicht geladen werden. Bitte erneut versuchen oder den Datendienst prüfen.'
      : null,
  );
  protected readonly pointError = computed(() =>
    this.measuringPointResource.status() === 'error'
      ? 'Der zugeordnete Messpunkt konnte nicht geladen werden.'
      : null,
  );
  protected readonly stationError = computed(() =>
    this.stationResource.status() === 'error'
      ? 'Die zugeordnete Pegelstelle konnte nicht geladen werden.'
      : null,
  );
  protected readonly ownerError = computed(() =>
    this.stationOwnerResource.status() === 'error'
      ? 'Die zugeordnete Organisation konnte nicht geladen werden.'
      : null,
  );

  constructor() {
    effect(() => {
      const pointName = this.measuringPoint()?.name;
      const measurementTypeLabel = this.measurementTypeLabel();
      const title = [pointName, measurementTypeLabel, 'PegelHub'].filter(Boolean).join(' · ');

      this.titleService.setTitle(title || 'Messreihe · PegelHub');
    });

    effect(() => {
      this.measurements.setTimeSeries(this.timeSeries());
    });
  }
}
