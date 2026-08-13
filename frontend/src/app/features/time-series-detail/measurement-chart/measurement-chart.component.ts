import { Component, computed, input } from '@angular/core';

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
  readonly series = input<PhChartSeries | null>(null);
  readonly referenceLines = input<readonly PhChartReferenceLine[]>([]);
  readonly yLabel = input<string>();
  readonly unit = input<string | null>(null);
  readonly loading = input(false);
  readonly emptyMessage = input('Für diesen Zeitraum sind noch keine Messwerte vorhanden.');
  readonly height = input('420');

  protected readonly updateLabel = UPDATE_LABEL;
  protected readonly pointCountLabel = computed(() => {
    const count = this.series()?.points.length ?? 0;

    return count === 1 ? '1 Datenpunkt' : `${count} Datenpunkte`;
  });
}
