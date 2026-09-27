import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import type { ChartOptions } from 'chart.js';
import { UIChart } from 'primeng/chart';
import { describe, expect, it } from 'vitest';

import { PhLineChartComponent } from './line-chart.component';

describe('PhLineChartComponent', () => {
  it('renders an accessible empty state when no points are available', () => {
    const fixture = TestBed.createComponent(PhLineChartComponent);
    fixture.componentRef.setInput('emptyMessage', 'Keine Messpunkte vorhanden.');
    fixture.detectChanges();

    const emptyState = fixture.debugElement.query(By.css('.ph-chart-empty'));

    expect(emptyState.attributes['role']).toBe('status');
    expect(emptyState.nativeElement.textContent).toContain('Keine Messpunkte vorhanden.');
  });

  it('creates labeled annotations and includes them in the visible y-axis range', () => {
    const fixture = TestBed.createComponent(PhLineChartComponent);
    fixture.componentRef.setInput('series', {
      name: 'Wasserstand',
      points: [point(280), point(300)],
    });
    fixture.componentRef.setInput('referenceLines', [
      { label: 'RNW 2020', value: 162, tone: 'lower' },
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
    fixture.componentRef.setInput('unit', 'cm');

    fixture.detectChanges();

    const chart = fixture.debugElement.query(By.directive(UIChart)).injector.get(UIChart);
    const options: ChartOptions<'line'> = chart.options;
    const annotations = options.plugins?.annotation?.annotations;

    expect(annotations).toEqual([
      expect.objectContaining({
        type: 'line',
        value: 162,
        label: expect.objectContaining({ content: 'RNW 2020 · 162 cm' }),
      }),
      expect.objectContaining({
        type: 'line',
        value: 480,
        label: expect.objectContaining({ content: 'HSW 2020 · 480 cm' }),
      }),
    ]);
    expect(options.scales?.['y']?.min).toBeLessThanOrEqual(162);
    expect(options.scales?.['y']?.max).toBeGreaterThanOrEqual(480);
    expect(options.scales?.['x']?.type).toBe('linear');
  });

  it('uses elapsed coordinates, retains the visible window and breaks missing intervals', () => {
    const fixture = TestBed.createComponent(PhLineChartComponent);
    fixture.componentRef.setInput('series', {
      name: 'Wasserstand',
      window: { from: 0, to: 4 * 3_600_000 },
      points: [point(301, 0, 3_600_000, 'Interval metadata'), point(305, 7_200_000, 10_800_000)],
    });
    fixture.detectChanges();

    const chart = fixture.debugElement.query(By.directive(UIChart)).injector.get(UIChart);
    const dataset = chart.data.datasets[0];
    const options = chart.options as ChartOptions<'line'>;

    expect(dataset.data).toEqual([
      expect.objectContaining({ x: 1_800_000, y: 301, tooltipFooter: 'Interval metadata' }),
      expect.objectContaining({ x: 3_600_000, y: null }),
      expect.objectContaining({ x: 7_200_000, y: null }),
      expect.objectContaining({ x: 9_000_000, y: 305 }),
    ]);
    expect(options.scales?.['x']).toMatchObject({
      type: 'linear',
      min: 0,
      max: 4 * 3_600_000,
    });
    expect(dataset.label).toBe('Wasserstand');
  });

  it('formats chart labels and tooltip boundaries in UTC when selected', () => {
    const fixture = TestBed.createComponent(PhLineChartComponent);
    fixture.componentRef.setInput('timeBasis', 'UTC');
    fixture.componentRef.setInput('series', {
      name: 'Wasserstand',
      window: { from: Date.parse('2026-07-19T10:00:00Z'), to: Date.parse('2026-07-19T12:00:00Z') },
      points: [point(301, Date.parse('2026-07-19T10:00:00Z'), Date.parse('2026-07-19T11:00:00Z'))],
    });
    fixture.detectChanges();
    const chart = fixture.debugElement.query(By.directive(UIChart)).injector.get(UIChart);
    const options = chart.options as ChartOptions<'line'>;
    const x = options.scales?.['x'];
    expect(
      Reflect.apply(x?.ticks?.callback as Function, {}, [
        Date.parse('2026-07-19T10:00:00Z'),
        0,
        [],
      ]),
    ).toContain('10:00');
    expect(
      Reflect.apply(options.plugins?.tooltip?.callbacks?.title as Function, {}, [
        [
          {
            raw: {
              from: Date.parse('2026-07-19T10:00:00Z'),
              to: Date.parse('2026-07-19T11:00:00Z'),
            },
          },
        ],
      ]),
    ).toContain('10:00');
  });

  it('shows isolated markers above the dense-point limit but hides connected markers', () => {
    const fixture = TestBed.createComponent(PhLineChartComponent);
    const singletonPoints = Array.from({ length: 20 }, (_, index) => {
      const from = index * 60 * 60_000;
      return point(300 + index, from, from + 15 * 60_000);
    });
    fixture.componentRef.setInput('series', {
      name: 'Wasserstand',
      window: { from: 0, to: 24 * 60 * 60_000 },
      points: singletonPoints,
    });
    fixture.detectChanges();

    const chart = fixture.debugElement.query(By.directive(UIChart)).injector.get(UIChart);
    const dataset = chart.data.datasets[0];
    const radius = dataset.pointRadius as (context: {
      dataIndex: number;
      dataset: { data: unknown[] };
      raw: unknown;
    }) => number;
    const data = dataset.data as unknown[];
    const validIndexes = data.flatMap((datum, index) =>
      datum && typeof datum === 'object' && 'y' in datum && datum.y !== null ? [index] : [],
    );

    expect(validIndexes).toHaveLength(20);
    expect(
      validIndexes.every(
        (dataIndex) => radius({ dataIndex, dataset: { data }, raw: data[dataIndex] }) > 0,
      ),
    ).toBe(true);

    const connectedPoints = Array.from({ length: 20 }, (_, index) =>
      point(300 + index, index * 15 * 60_000, (index + 1) * 15 * 60_000),
    );
    fixture.componentRef.setInput('series', {
      name: 'Wasserstand',
      points: connectedPoints,
    });
    fixture.detectChanges();

    const connectedDataset = chart.data.datasets[0];
    const connectedRadius = connectedDataset.pointRadius as typeof radius;
    const connectedData = connectedDataset.data as unknown[];
    expect(
      connectedData.map((raw, dataIndex) =>
        connectedRadius({ dataIndex, dataset: { data: connectedData }, raw }),
      ),
    ).toEqual(Array(20).fill(0));
  });
});

function point(value: number, from = 0, to = 3_600_000, tooltipFooter?: string) {
  return { x: (from + to) / 2, from, to, value, tooltipFooter };
}
