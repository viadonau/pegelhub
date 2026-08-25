import { Component, computed, inject, input } from '@angular/core';
import { Chart, type ChartData, type ChartOptions } from 'chart.js';
import annotationPlugin, { type AnnotationOptions } from 'chartjs-plugin-annotation';
import { ChartModule } from 'primeng/chart';

import { ThemeMode, ThemeService } from '../../core/theme/theme.service';

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

interface ChartAxisBounds {
  min: number;
  max: number;
  precision: number;
  stepSize: number;
}

const Y_AXIS_TARGET_INTERVALS = 4;
const WATER_LEVEL_MIN_VISIBLE_SPAN_CM = 10;
const WATER_LEVEL_MIN_VISIBLE_SPAN_M = 0.1;
const DEFAULT_RELATIVE_MIN_VISIBLE_SPAN = 0.02;
const DEFAULT_MIN_VISIBLE_SPAN = 1;
const LOW_DENSITY_POINT_LIMIT = 16;
const CHART_FONT_FAMILY =
  "'Source Sans 3 Variable', ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";

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
      font: {
        family: CHART_FONT_FAMILY,
      },
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

function createYAxisBounds(
  values: number[],
  unit: string | null | undefined,
): ChartAxisBounds | null {
  const finiteValues = values.filter(Number.isFinite);

  if (finiteValues.length === 0) {
    return null;
  }

  const dataMin = Math.min(...finiteValues);
  const dataMax = Math.max(...finiteValues);
  const dataSpan = dataMax - dataMin;
  const center = (dataMin + dataMax) / 2;
  const minimumVisibleSpan = getMinimumVisibleSpan(unit, center);
  const targetSpan = Math.max(dataSpan * 1.3, minimumVisibleSpan);
  const stepSize = niceNumber(targetSpan / Y_AXIS_TARGET_INTERVALS);
  const halfSpan = targetSpan / 2;
  let min = Math.floor((center - halfSpan) / stepSize) * stepSize;
  let max = Math.ceil((center + halfSpan) / stepSize) * stepSize;

  if (dataMin >= 0 && min < 0) {
    min = 0;
    max = Math.ceil(Math.max(dataMax, min + targetSpan) / stepSize) * stepSize;
  }

  if (min === max) {
    max = min + stepSize * Y_AXIS_TARGET_INTERVALS;
  }

  const precision = precisionForStep(stepSize);

  return {
    min: normalizeAxisNumber(min, precision),
    max: normalizeAxisNumber(max, precision),
    precision,
    stepSize: normalizeAxisNumber(stepSize, precision),
  };
}

function getMinimumVisibleSpan(unit: string | null | undefined, center: number): number {
  const normalizedUnit = normalizeUnit(unit);

  if (normalizedUnit === 'cm') {
    return WATER_LEVEL_MIN_VISIBLE_SPAN_CM;
  }

  if (isMeterUnit(normalizedUnit)) {
    return WATER_LEVEL_MIN_VISIBLE_SPAN_M;
  }

  const relativeSpan = niceNumber(Math.abs(center) * DEFAULT_RELATIVE_MIN_VISIBLE_SPAN);

  return Math.max(relativeSpan, DEFAULT_MIN_VISIBLE_SPAN);
}

function normalizeUnit(unit: string | null | undefined): string {
  return unit?.trim().toLowerCase().replace(/\s+/g, '') ?? '';
}

function isMeterUnit(normalizedUnit: string): boolean {
  return (
    normalizedUnit === 'm' ||
    normalizedUnit === 'meter' ||
    normalizedUnit === 'metre' ||
    normalizedUnit.startsWith('mü') ||
    normalizedUnit.startsWith('mue')
  );
}

function niceNumber(value: number): number {
  if (!Number.isFinite(value) || value <= 0) {
    return DEFAULT_MIN_VISIBLE_SPAN;
  }

  const exponent = Math.floor(Math.log10(value));
  const fraction = value / 10 ** exponent;
  let niceFraction: number;

  if (fraction < 1.5) {
    niceFraction = 1;
  } else if (fraction < 3) {
    niceFraction = 2;
  } else if (fraction < 7) {
    niceFraction = 5;
  } else {
    niceFraction = 10;
  }

  return niceFraction * 10 ** exponent;
}

function precisionForStep(step: number): number {
  if (!Number.isFinite(step) || step <= 0) {
    return 0;
  }

  return Math.max(0, Math.ceil(-Math.log10(step)));
}

function normalizeAxisNumber(value: number, precision: number): number {
  return Number(value.toFixed(precision + 2));
}

function formatChartNumber(value: number): string {
  return new Intl.NumberFormat('de-AT', {
    maximumFractionDigits: 3,
  }).format(value);
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

function formatReferenceLineLabel(
  line: PhChartReferenceLine,
  unit: string | null | undefined,
): string {
  const suffix = unit ? ` ${unit}` : '';

  return `${line.label} · ${formatChartNumber(line.value)}${suffix}`;
}
