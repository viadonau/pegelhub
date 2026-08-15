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

  it('loads chart data from the bucket endpoint', () => {
    const resource = TestBed.runInInjectionContext(() =>
      service.measurementBucketsResource(signal('series-id'), signal('7d')),
    );
    TestBed.tick();

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/v1/time-series/series-id/measurements/buckets',
    );

    expect(request.request.method).toBe('GET');
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
});
