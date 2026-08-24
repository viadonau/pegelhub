import { HttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createAppConfig } from './app.config';
import { TEST_RUNTIME_CONFIG } from '../testing/fixtures';

describe('application authentication configuration', () => {
  let http: HttpTestingController;

  afterEach(() => http.verify());

  it('sends the bearer token only to the configured Core API boundary', async () => {
    const keycloak = {
      authenticated: true,
      init: vi.fn().mockResolvedValue(true),
      token: 'operator-token',
      updateToken: vi.fn().mockResolvedValue(true),
    };
    TestBed.configureTestingModule({
      providers: [...createAppConfig(TEST_RUNTIME_CONFIG).providers, provideHttpClientTesting()],
    });
    TestBed.overrideProvider(Keycloak, { useValue: keycloak });
    const client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);

    client.get('/api/v1/monitoring/time-series').subscribe();
    const coreRequest = await waitForRequest('/api/v1/monitoring/time-series');
    expect(coreRequest.request.headers.get('Authorization')).toBe('Bearer operator-token');
    coreRequest.flush({});

    for (const url of [
      '/api/v10/monitoring/time-series',
      '/other/api/v1/monitoring/time-series',
      'https://third-party.test/api/v1/monitoring/time-series',
    ]) {
      client.get(url).subscribe();
      const request = http.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({});
    }
  });

  async function waitForRequest(url: string): Promise<TestRequest> {
    let request: TestRequest | undefined;

    await vi.waitFor(() => {
      const matches = http.match(url);
      expect(matches).toHaveLength(1);
      request = matches[0];
    });

    return request!;
  }
});
