import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import type { ChartOptions } from 'chart.js';
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
    expect(emptyState.query(By.css('.pi-chart-line'))).not.toBeNull();
  });

  it('creates labeled annotations and includes them in the visible y-axis range', () => {
    const fixture = TestBed.createComponent(PhLineChartComponent);
    fixture.componentRef.setInput('series', {
      name: 'Wasserstand',
      points: [
        { label: '10:00', value: 280 },
        { label: '11:00', value: 300 },
      ],
    });
    fixture.componentRef.setInput('referenceLines', [
      { label: 'RNW 2020', value: 162, tone: 'lower' },
      { label: 'HSW 2020', value: 480, tone: 'upper' },
    ]);
    fixture.componentRef.setInput('unit', 'cm');

    const viewModel = fixture.componentInstance as unknown as {
      chartOptions: () => ChartOptions<'line'>;
      yAxisBounds: () => { min: number; max: number } | null;
    };
    const options = viewModel.chartOptions();
    const annotations = options.plugins?.annotation?.annotations;

    expect(options.font?.family).toContain('Source Sans 3 Variable');
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
    expect(viewModel.yAxisBounds()?.min).toBeLessThanOrEqual(162);
    expect(viewModel.yAxisBounds()?.max).toBeGreaterThanOrEqual(480);
  });
});
