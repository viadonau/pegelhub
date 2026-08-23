import { Component, computed, inject, input } from '@angular/core';
import { Chart, type ChartData, type ChartOptions } from 'chart.js';
import annotationPlugin, { type AnnotationOptions } from 'chartjs-plugin-annotation';
import { ChartModule } from 'primeng/chart';

import { ThemeMode, ThemeService } from '../../core/theme/theme.service';
import {
  createYAxisBounds,
  formatChartNumber,
  formatReferenceLineLabel,
} from './line-chart.presentation';

Chart.register(annotationPlugin);

export interface PhChartPoint {
  label: string;
  value: number;
}

export interface PhChartSeries {
  name: string;
  points: PhChartPoint[];
}

export interface PhChartReferenceLine {
  label: string;
  value: number;
  tone?: 'lower' | 'upper';
}

const LOW_DENSITY_POINT_LIMIT = 16;

@Component({
  selector: 'ph-line-chart',
  imports: [ChartModule],
  host: {
    class: 'block',
  },
  template: `
    @if (!series() || pointCount() === 0) {
      <div class="ph-chart-empty" role="status">
        <i class="pi pi-chart-line" aria-hidden="true"></i>
        <p>{{ emptyMessage() }}</p>
      </div>
    } @else {
      <p-chart
        type="line"
        [height]="height()"
        [data]="chartData()"
        [options]="chartOptions()"
        [ariaLabel]="ariaLabel()"
      />
    }
  `,
  styleUrl: './line-chart.component.scss',
})
export class PhLineChartComponent {
  private readonly theme = inject(ThemeService);

  readonly series = input<PhChartSeries | null>(null);
  readonly referenceLines = input<readonly PhChartReferenceLine[]>([]);
  readonly yLabel = input<string>();
  readonly unit = input<string | null>();
  readonly emptyMessage = input('Keine Diagrammdaten vorhanden.');
  readonly ariaLabel = input('Liniendiagramm');
  readonly height = input('320');

  protected readonly pointCount = computed(() => this.series()?.points.length ?? 0);
  protected readonly yAxisBounds = computed(() =>
    createYAxisBounds(
      [
        ...(this.series()?.points.map((point) => point.value) ?? []),
        ...this.referenceLines().map((line) => line.value),
      ],
      this.unit(),
    ),
  );

  protected readonly chartData = computed<ChartData<'line'>>(() => {
    const series = this.series();
    const pointRadius = this.pointCount() <= LOW_DENSITY_POINT_LIMIT ? 2 : 0;
    const tokens = chartThemes[this.theme.mode()];

    return {
      labels: series?.points.map((point) => point.label) ?? [],
      datasets: series
        ? [
            {
              label: series.name,
              data: series.points.map((point) => point.value),
              borderColor: tokens.series,
              backgroundColor: tokens.series,
              borderWidth: 2,
              borderCapStyle: 'round',
              borderJoinStyle: 'round',
              pointBackgroundColor: tokens.surface,
              pointBorderColor: tokens.series,
              pointBorderWidth: 2,
              pointRadius,
              pointHoverBackgroundColor: tokens.surface,
              pointHoverBorderColor: tokens.series,
              pointHoverBorderWidth: 2,
              pointHoverRadius: 4,
              pointHitRadius: 14,
              tension: 0,
            },
          ]
        : [],
    };
  });

  protected readonly chartOptions = computed<ChartOptions<'line'>>(() => {
    const yAxisBounds = this.yAxisBounds();
    const tokens = chartThemes[this.theme.mode()];

    return {
      animation: false,
      maintainAspectRatio: false,
      layout: {
        padding: {
          top: 6,
          right: 6,
        },
      },
      plugins: {
        legend: {
          display: false,
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          backgroundColor: tokens.tooltipBackground,
          titleColor: tokens.tooltipText,
          bodyColor: tokens.tooltipText,
          cornerRadius: 6,
          caretPadding: 8,
          caretSize: 5,
          padding: 10,
          displayColors: false,
          boxPadding: 4,
          titleFont: { size: 12, weight: 600 },
          bodyFont: { size: 12, weight: 500 },
          callbacks: {
            label: (ctx) => {
              const unit = this.unit();
              const formattedValue =
                typeof ctx.parsed.y === 'number' ? formatChartNumber(ctx.parsed.y) : '-';

              return formattedValue + (unit ? ` ${unit}` : '');
            },
          },
        },
        annotation: {
          annotations: createReferenceAnnotations(this.referenceLines(), this.unit(), tokens),
        },
      },
      interaction: {
        mode: 'nearest',
        intersect: false,
      },
      scales: {
        x: {
          ticks: {
            maxRotation: 0,
            autoSkip: true,
            maxTicksLimit: 8,
            autoSkipPadding: 24,
            padding: 8,
            color: tokens.textMuted,
            font: { size: 11 },
          },
          grid: {
            display: false,
          },
          border: {
            color: tokens.axis,
          },
        },
        y: {
          beginAtZero: false,
          min: yAxisBounds?.min,
          max: yAxisBounds?.max,
          title: {
            display: Boolean(this.yLabel()),
            text: this.yLabel(),
            color: tokens.textMuted,
            font: { size: 12, weight: 500 },
          },
          ticks: {
            color: tokens.textMuted,
            font: { size: 11 },
            maxTicksLimit: 6,
            padding: 10,
            precision: yAxisBounds?.precision ?? 0,
            stepSize: yAxisBounds?.stepSize,
            callback: (value) => formatChartNumber(Number(value)),
          },
          grid: {
            color: tokens.grid,
            drawTicks: false,
          },
          border: {
            display: false,
          },
        },
      },
    };
  });
}

interface ChartTheme {
  series: string;
  referenceLines: Record<'lower' | 'upper', string>;
  textMuted: string;
  surface: string;
  axis: string;
  grid: string;
  tooltipBackground: string;
  tooltipText: string;
}

const chartThemes: Record<ThemeMode, ChartTheme> = {
  light: {
    series: '#4691af',
    referenceLines: { lower: '#007d69', upper: '#a84f22' },
    textMuted: '#5b6b75',
    surface: '#ffffff',
    axis: '#cbd6dd',
    grid: '#e4eaee',
    tooltipBackground: '#0b1820',
    tooltipText: '#ffffff',
  },
  dark: {
    series: '#79b9d3',
    referenceLines: { lower: '#58b5a2', upper: '#e29a72' },
    textMuted: '#b8c7ce',
    surface: '#102a35',
    axis: '#526a74',
    grid: '#304852',
    tooltipBackground: '#eef4f6',
    tooltipText: '#10242d',
  },
};

function createReferenceAnnotations(
  lines: readonly PhChartReferenceLine[],
  unit: string | null | undefined,
  theme: ChartTheme,
): AnnotationOptions<'line'>[] {
  return lines.map((line) => {
    const color = theme.referenceLines[line.tone ?? 'lower'];

    return {
      type: 'line',
      scaleID: 'y',
      value: line.value,
      borderColor: color,
      borderWidth: 1.5,
      borderDash: [6, 4],
      drawTime: 'afterDatasetsDraw',
      label: {
        display: true,
        content: formatReferenceLineLabel(line, unit),
        position: 'end',
        xAdjust: -6,
        padding: { x: 6, y: 3 },
        borderColor: color,
        borderWidth: 1,
        borderRadius: 3,
        backgroundColor: theme.surface,
        color,
        font: { size: 11, weight: 600 },
      },
    };
  });
}
