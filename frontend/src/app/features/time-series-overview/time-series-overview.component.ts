import { Component, computed, inject, signal } from '@angular/core';

import { MonitoringApiService } from '../../core/api/monitoring-api.service';
import { PhPageHeaderComponent } from '../../ui/page/page-header.component';
import { PhPageSectionComponent } from '../../ui/page/page-section.component';
import { PhPageComponent } from '../../ui/page/page.component';
import { timeSeriesOverviewViews } from './model/time-series-overview';
import { PhTimeSeriesGridComponent } from './time-series-grid/time-series-grid.component';

@Component({
  selector: 'app-time-series-overview',
  imports: [
    PhPageComponent,
    PhPageHeaderComponent,
    PhPageSectionComponent,
    PhTimeSeriesGridComponent,
  ],
  templateUrl: './time-series-overview.component.html',
  styleUrl: './time-series-overview.component.scss',
})
export class TimeSeriesOverviewComponent {
  private readonly monitoringApi = inject(MonitoringApiService);
  private readonly collectionResource = this.monitoringApi.timeSeriesCollectionResource();
  private readonly visibleRowCount = signal<number | null>(null);

  protected readonly rows = computed(() =>
    timeSeriesOverviewViews(
      this.collectionResource.hasValue() ? this.collectionResource.value().items : [],
    ),
  );
  private readonly visibleCount = computed(() => this.visibleRowCount() ?? this.rows().length);

  protected readonly resultSummary = computed(() => {
    const count = this.rows().length;
    const visibleCount = this.visibleCount();

    return visibleCount !== count
      ? `${visibleCount} von ${count} Messreihen`
      : count === 1
        ? '1 Messreihe'
        : `${count} Messreihen`;
  });
  protected readonly emptyMessage = computed(() =>
    this.rows().length > 0 && this.visibleCount() === 0
      ? 'Keine Messreihe entspricht den Filtern.'
      : 'Keine Messreihen vorhanden.',
  );
  protected readonly loading = this.collectionResource.isLoading;
  protected readonly error = computed(() =>
    this.collectionResource.status() === 'error'
      ? 'Die Messreihenübersicht konnte nicht geladen werden. Bitte erneut versuchen oder den Datendienst prüfen.'
      : null,
  );

  protected updateVisibleRowCount(count: number): void {
    this.visibleRowCount.set(count);
  }
}
