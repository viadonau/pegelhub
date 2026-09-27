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

  afterEach(() => http.verify());

  it('loads aligned means from the interval endpoint', () => {
    const resource = TestBed.runInInjectionContext(() =>
      service.measurementIntervalsResource(
        signal('series-id'),
        signal({
          from: '2026-06-17T00:00:00Z',
          to: '2026-06-18T00:00:00Z',
          interval: '15m' as const,
          timeBasis: '+01:00' as const,
        }),
      ),
    );
    TestBed.tick();

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/v1/time-series/series-id/measurements/intervals',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('interval')).toBe('15m');
    const wireParams = new URLSearchParams(request.request.params.toString());
    expect(wireParams.get('timeBasis')).toBe('+01:00');
    expect(request.request.params.get('closedOnly')).toBe('true');
    request.flush({
      timeSeriesId: 'series-id',
      from: '2026-06-17T00:00:00Z',
      to: '2026-06-18T00:00:00Z',
      interval: '15m',
      timeBasis: '+01:00',
      closedOnly: true,
      representation: 'canonical',
      unit: 'cm',
      method: 'time-weighted-step',
      computedAt: '2026-06-18T00:00:00Z',
      intervals: [],
    });
    resource.destroy();
  });
});
