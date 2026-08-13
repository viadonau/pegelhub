import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { MemoryStorage } from '../../../../testing/memory-storage';
import { MeasurementChartPreferences } from './measurement-chart-preferences.service';

describe('MeasurementChartPreferences', () => {
  let storage: Storage;

  beforeEach(() => {
    storage = new MemoryStorage();
    Object.defineProperty(window, 'localStorage', { configurable: true, value: storage });
  });

  it('keeps reference levels hidden by default', () => {
    const preferences = TestBed.inject(MeasurementChartPreferences);

    expect(preferences.showReferenceLevels()).toBe(false);
  });

  it('persists the selected reference-level visibility', () => {
    const preferences = TestBed.inject(MeasurementChartPreferences);

    preferences.setReferenceLevelsVisible(true);

    expect(preferences.showReferenceLevels()).toBe(true);
    expect(storage.getItem('pegelhub.chart.referenceLevels')).toBe('true');
  });

  it('restores a persisted choice', () => {
    storage.setItem('pegelhub.chart.referenceLevels', 'true');

    expect(TestBed.inject(MeasurementChartPreferences).showReferenceLevels()).toBe(true);
  });
});
