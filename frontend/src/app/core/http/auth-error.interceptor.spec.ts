import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEvent, KeycloakEventType } from 'keycloak-angular';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { authErrorInterceptor } from './auth-error.interceptor';

@Component({ template: '' })
class TestRouteComponent {}

describe('authErrorInterceptor', () => {
  const keycloak = {
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    tokenParsed: undefined,
  };
  const keycloakEvent = signal<KeycloakEvent>({
    type: KeycloakEventType.Ready,
    args: true,
  });
  let client: HttpClient;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    keycloak.login.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authErrorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'overview/:id', component: TestRouteComponent },
          { path: 'forbidden', component: TestRouteComponent },
        ]),
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEvent },
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    await router.navigateByUrl('/overview/series-water-level');
  });

  afterEach(() => http.verify());

  it('starts login for a 401 and preserves the current route', async () => {
    const response = failedRequest(401, 'Unauthorized');

    await expect(response).rejects.toMatchObject({ status: 401 });
    expect(keycloak.login).toHaveBeenCalledWith({
      redirectUri: new URL('/overview/series-water-level', window.location.origin).toString(),
    });
  });

  it('replaces the current route with the forbidden page for a 403', async () => {
    const navigate = vi.spyOn(router, 'navigateByUrl');
    const response = failedRequest(403, 'Forbidden');

    await expect(response).rejects.toMatchObject({ status: 403 });
    await vi.waitFor(() => expect(router.url).toBe('/forbidden'));
    expect(navigate).toHaveBeenCalledWith('/forbidden', { replaceUrl: true });
    expect(keycloak.login).not.toHaveBeenCalled();
  });

  it('does not create a forbidden redirect loop', async () => {
    await router.navigateByUrl('/forbidden');
    const navigate = vi.spyOn(router, 'navigateByUrl');
    const response = failedRequest(403, 'Forbidden');

    await expect(response).rejects.toMatchObject({ status: 403 });
    expect(navigate).not.toHaveBeenCalled();
  });

  it('propagates unrelated failures unchanged and without authentication side effects', async () => {
    const navigate = vi.spyOn(router, 'navigateByUrl');
    const response = failedRequest(503, 'Service Unavailable');

    await expect(response).rejects.toMatchObject({
      error: 'failed',
      status: 503,
      statusText: 'Service Unavailable',
      url: '/api/v1/protected',
    });
    expect(keycloak.login).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  function failedRequest(status: number, statusText: string): Promise<unknown> {
    const response = firstValueFrom(client.get('/api/v1/protected'));
    http.expectOne('/api/v1/protected').flush('failed', { status, statusText });
    return response;
  }
});
