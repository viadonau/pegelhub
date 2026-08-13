import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { PhTimeSeriesMetadataComponent } from './time-series-metadata.component';

describe('PhTimeSeriesMetadataComponent', () => {
  it('renders point and selected-series metadata as two groups in one band', () => {
    const fixture = TestBed.createComponent(PhTimeSeriesMetadataComponent);
    fixture.componentRef.setInput('measuringPointFacts', [
      { label: 'Organisation', value: 'viadonau' },
      { label: 'Ufer', value: 'rechts' },
      { label: 'MW 2020', value: '315,5 cm' },
    ]);
    fixture.componentRef.setInput('timeSeriesFacts', [
      { label: 'Externer Schlüssel', value: 'W-001' },
    ]);
    fixture.detectChanges();

    const groups = fixture.nativeElement.querySelectorAll('.ph-time-series-metadata-group');
    const text = fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();

    expect(groups).toHaveLength(2);
    expect(text).toContain('Messpunkt');
    expect(text).toContain('Messreihe');
    expect(text).toContain('315,5 cm');
  });
});
