import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeasuringPointApiService } from '../../../core/api/measuring-point-api.service';
import { StationApiService } from '../../../core/api/station-api.service';
import { TimeSeriesApiService } from '../../../core/api/time-series-api.service';
import { MeasuringPointDto } from '../../../core/api/measuring-point.dto';
import { StationDto } from '../../../core/api/station.dto';
import { TimeSeriesDto } from '../../../core/api/time-series.dto';
import { OverviewLatestMeasurementsService } from './overview-latest-measurements.service';
import { TimeSeriesOverviewState } from './time-series-overview.state';

describe('TimeSeriesOverviewState', () => {
  const point: MeasuringPointDto = { id: 'point-1', stationId: 'station-1', name: 'Hauptpegel' };
  const station: StationDto = {
    id: 'station-1',
    ownerId: 'owner-1',
    stationNumber: '10001030',
    name: 'Wien Brigittenau',
    waterBody: 'Donau',
  };
  const series: TimeSeriesDto = {
    id: 'series-w',
    measuringPointId: point.id,
    observedProperty: 'water-level',
    unit: 'cm',
  };

  let pointsResource: ReturnType<typeof fakeResource>;
  let stationsResource: ReturnType<typeof fakeResource>;
  let seriesResource: ReturnType<typeof fakeResource>;
  let loadLatest: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    pointsResource = fakeResource([point]);
    stationsResource = fakeResource([station]);
    seriesResource = fakeResource([series]);
    loadLatest = vi.fn(() =>
      of(
        new Map([
          [
            series.id,
            {
              status: 'available' as const,
              measurement: { observedAt: '2026-07-22T10:00:00Z', value: 312.5 },
            },
          ],
        ]),
      ),
    );

    TestBed.configureTestingModule({
      providers: [
        TimeSeriesOverviewState,
        {
          provide: MeasuringPointApiService,
          useValue: { measuringPointsResource: vi.fn(() => pointsResource) },
        },
        {
          provide: StationApiService,
          useValue: { stationsResource: vi.fn(() => stationsResource) },
        },
        {
          provide: TimeSeriesApiService,
          useValue: { timeSeriesListResource: vi.fn(() => seriesResource) },
        },
        {
          provide: OverviewLatestMeasurementsService,
          useValue: { load: loadLatest },
        },
      ],
    });
  });

  it('owns the complete overview projection and latest-value query', () => {
    const state = TestBed.inject(TimeSeriesOverviewState);
    TestBed.flushEffects();

    expect(loadLatest).toHaveBeenCalledWith([series.id]);
    expect(state.rows()[0]).toMatchObject({
      id: series.id,
      measuringPointName: 'Hauptpegel',
      latestMeasurement: { valueLabel: '312,5 cm' },
    });
  });

  it('keeps overview loading and filtering state as local computed state', () => {
    const state = TestBed.inject(TimeSeriesOverviewState);

    state.updateVisibleRowCount(0);

    expect(state.resultSummary()).toBe('0 von 1 Messreihen');
    expect(state.emptyMessage()).toBe('Keine Messreihe entspricht den Filtern.');

    pointsResource.status.set('error');

    expect(state.errorMessage()).toContain('Messpunkte konnten nicht geladen werden');
  });
});

function fakeResource<T>(initialValue: T) {
  return {
    value: signal(initialValue),
    hasValue: () => true,
    status: signal('resolved'),
    isLoading: signal(false),
    reload: vi.fn(),
  };
}
