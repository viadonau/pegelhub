import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import {
  AutoRefreshTokenService,
  KEYCLOAK_EVENT_SIGNAL,
  KeycloakEvent,
  KeycloakEventType,
  UserActivityService,
} from 'keycloak-angular';
import { describe, expect, it, vi } from 'vitest';

import { createAppConfig, KEYCLOAK_SESSION_IDLE_TIMEOUT_MS } from './app.config';
import { RuntimeConfig } from './core/config/runtime-config';

const runtimeConfig: RuntimeConfig = {
  apiBaseUrl: '/api',
  keycloak: {
    url: 'http://keycloak.test',
    realm: 'pegelhub',
    clientId: 'pegelhub-frontend',
  },
};

@Component({ template: '' })
class TestHostComponent {}

describe('application authentication configuration', () => {
  it('starts proactive Keycloak session handling during application initialization', async () => {
    const appConfig = createAppConfig(runtimeConfig);
    TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: appConfig.providers,
    });
    const startSessionHandling = vi.fn();
    TestBed.overrideProvider(Keycloak, {
      useValue: { init: vi.fn().mockResolvedValue(true) },
    });
    TestBed.overrideProvider(AutoRefreshTokenService, {
      useValue: { start: startSessionHandling },
    });

    const fixture = TestBed.createComponent(TestHostComponent);
    await fixture.whenStable();

    expect(startSessionHandling).toHaveBeenCalledWith({
      onInactivityTimeout: 'login',
      sessionTimeout: KEYCLOAK_SESSION_IDLE_TIMEOUT_MS,
    });
  });

  it('redirects to Keycloak when an expired token can no longer be refreshed', async () => {
    const keycloakEvent = signal<KeycloakEvent>({
      type: KeycloakEventType.Ready,
      args: true,
    });
    const keycloak = {
      authenticated: true,
      login: vi.fn().mockResolvedValue(undefined),
      updateToken: vi.fn().mockRejectedValue(new Error('Session expired')),
    };
    const userActivity = {
      isActive: vi.fn().mockReturnValue(true),
      startMonitoring: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        AutoRefreshTokenService,
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEvent },
        { provide: UserActivityService, useValue: userActivity },
      ],
    });

    TestBed.inject(AutoRefreshTokenService).start({
      onInactivityTimeout: 'login',
      sessionTimeout: KEYCLOAK_SESSION_IDLE_TIMEOUT_MS,
    });
    keycloakEvent.set({ type: KeycloakEventType.TokenExpired });
    TestBed.flushEffects();

    await vi.waitFor(() => {
      expect(keycloak.updateToken).toHaveBeenCalledOnce();
      expect(keycloak.login).toHaveBeenCalledOnce();
    });
  });
});
