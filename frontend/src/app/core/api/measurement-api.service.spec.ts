import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { RUNTIME_CONFIG, RuntimeConfig } from '../config/runtime-config';

import { MeasurementApiService } from './measurement-api.service';

const TEST_RUNTIME_CONFIG: RuntimeConfig = {
  apiBaseUrl: '/api/v1',
  keycloak: {
    url: 'http://keycloak.test',
    realm: 'pegelhub',
    clientId: 'pegelhub-frontend',
  },
};

describe('MeasurementApiService HTTP contract', () => {
  let service: MeasurementApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RUNTIME_CONFIG, useValue: TEST_RUNTIME_CONFIG },
      ],
    });

    service = TestBed.inject(MeasurementApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('loads chart data from the bucket endpoint', () => {
    const resource = service.measurementBucketsResource(signal('series-id'), signal('7d'));
    TestBed.tick();

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/v1/time-series/series-id/measurements/buckets',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['last', 'maxPoints']);
    expect(request.request.params.get('last')).toBe('7d');
    expect(request.request.params.get('maxPoints')).toBe('240');

    request.flush({
      timeSeriesId: 'series-id',
      window: null,
      resolution: null,
      points: [],
    });
    resource.destroy();
  });

  it('loads the latest value independently from the selected history range', () => {
    const resource = service.latestMeasurementResource(signal('series-id'));
    TestBed.tick();

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/v1/time-series/series-id/measurements',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['last', 'limit', 'order']);
    expect(request.request.params.get('last')).toBe('365d');
    expect(request.request.params.get('order')).toBe('desc');
    expect(request.request.params.get('limit')).toBe('1');

    request.flush({
      timeSeriesId: 'series-id',
      window: null,
      order: 'desc',
      limit: 1,
      truncated: false,
      measurements: [],
    });
    resource.destroy();
  });

  it('fetches one latest value for overview monitoring', () => {
    let latestValue: number | null = null;

    service.latestMeasurement('series-id').subscribe((measurement) => {
      latestValue = measurement?.value ?? null;
    });

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/v1/time-series/series-id/measurements',
    );

    expect(request.request.params.get('last')).toBe('365d');
    expect(request.request.params.get('order')).toBe('desc');
    expect(request.request.params.get('limit')).toBe('1');

    request.flush({
      timeSeriesId: 'series-id',
      window: null,
      order: 'desc',
      limit: 1,
      truncated: false,
      measurements: [{ observedAt: '2026-07-20T10:00:00Z', value: 312.5 }],
    });

    expect(latestValue).toBe(312.5);
  });
});
