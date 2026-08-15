import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { RUNTIME_CONFIG, RuntimeConfig } from '../config/runtime-config';
import { MonitoringApiService } from './monitoring-api.service';

const TEST_RUNTIME_CONFIG: RuntimeConfig = {
  apiBaseUrl: '/api/v1',
  keycloak: { url: 'http://keycloak.test', realm: 'pegelhub', clientId: 'pegelhub-frontend' },
};

describe('MonitoringApiService HTTP contract', () => {
  let service: MonitoringApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RUNTIME_CONFIG, useValue: TEST_RUNTIME_CONFIG },
      ],
    });
    service = TestBed.inject(MonitoringApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('makes one collection request with the bounded latest window', () => {
    const resource = TestBed.runInInjectionContext(() => service.timeSeriesCollectionResource());
    TestBed.tick();

    const request = http.expectOne('/api/v1/monitoring/time-series?latestWithin=365d');
    expect(request.request.method).toBe('GET');
    request.flush({ items: [] });
    resource.destroy();
  });

  it('makes one detail snapshot request from the route signal', () => {
    const resource = TestBed.runInInjectionContext(() =>
      service.timeSeriesDetailResource(signal('series-1')),
    );
    TestBed.tick();

    const request = http.expectOne('/api/v1/monitoring/time-series/series-1?latestWithin=365d');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 'series-1' });
    resource.destroy();
  });
});
