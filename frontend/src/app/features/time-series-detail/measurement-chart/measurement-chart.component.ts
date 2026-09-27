import { Component, computed, input } from '@angular/core';

import type { MeasurementChartSeries } from '../model/measurement-view';
import {
  PhChartReferenceLine,
  PhChartSeries,
  PhLineChartComponent,
} from '../../../ui/chart/line-chart.component';

const UPDATE_LABEL = 'Messverlauf wird aktualisiert';

@Component({
  selector: 'ph-measurement-chart',
  imports: [PhLineChartComponent],
  host: {
    class: 'ph-measurement-chart',
    role: 'region',
    '[class.is-updating]': 'loading()',
    '[attr.aria-label]': 'ariaLabel()',
    '[attr.aria-busy]': 'loading()',
  },
  templateUrl: './measurement-chart.component.html',
  styleUrl: './measurement-chart.component.scss',
})
export class PhMeasurementChartComponent {
  readonly title = input('Messverlauf');
  readonly ariaLabel = input('Messverlauf des Messpunkts');
  readonly series = input<MeasurementChartSeries | null>(null);
  readonly referenceLines = input<readonly PhChartReferenceLine[]>([]);
  readonly yLabel = input<string>();
  readonly unit = input<string | null>(null);
  readonly loading = input(false);
  readonly emptyMessage = input('Für diesen Zeitraum sind noch keine Messwerte vorhanden.');
  readonly height = input('420');
  readonly timeBasis = input<'+01:00' | 'UTC'>('+01:00');
  readonly emptyIntervalCount = input(0);

  protected readonly updateLabel = UPDATE_LABEL;
  protected readonly lineChartSeries = computed<PhChartSeries | null>(() => {
    const series = this.series();
    if (!series) return null;

    return {
      name: series.name,
      window: series.window,
      points: series.points.map(({ observationCount, lastContributingObservedAt, ...point }) => ({
        ...point,
        tooltipFooter: formatObservationContext(
          observationCount,
          point.to,
          lastContributingObservedAt,
        ),
      })),
    };
  });

  protected readonly pointCountLabel = computed(() => {
    const series = this.series();
    const count = series?.points.length ?? 0;

    const intervals = count === 1 ? '1 Mittelwert' : `${count} Mittelwerte`;

    const resolution = series?.intervalLabel ? ` · ${series.intervalLabel} Raster` : '';

    const empty = this.emptyIntervalCount();
    const evidence = empty
      ? ` · ${empty} ${empty === 1 ? 'Intervall ohne vollständige Stützung' : 'Intervalle ohne vollständige Stützung'}`
      : '';
    return `${intervals}${evidence}${resolution} · ${this.timeBasis() === '+01:00' ? 'MEZ (UTC+1)' : 'UTC'}`;
  });
}

function formatObservationContext(
  observationCount: number,
  intervalEnd: number,
  lastObservedAt: string | null,
): string {
  if (observationCount > 0) {
    return observationCount === 1 ? '1 neuer Messwert' : `${observationCount} neue Messwerte`;
  }
  if (!lastObservedAt) {
    return 'Keine neuen Messwerte';
  }
  return `Fortgeführt · letzter Messwert ${formatObservationAge(intervalEnd, lastObservedAt)} alt`;
}

function formatObservationAge(intervalEnd: number, observedAt: string): string {
  const minutes = Math.floor((intervalEnd - Date.parse(observedAt)) / 60_000);
  if (minutes < 1) return 'unter 1 min';
  const hours = Math.floor(minutes / 60);
  return hours ? `${hours} h ${minutes % 60} min` : `${minutes} min`;
}
