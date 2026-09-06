import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEvent, KeycloakEventType } from 'keycloak-angular';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthStateService } from './auth-state.service';

describe('AuthStateService', () => {
  const keycloak = {
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    tokenParsed: undefined as Record<string, unknown> | undefined,
  };
  const keycloakEvent = signal<KeycloakEvent>({
    type: KeycloakEventType.Ready,
    args: true,
  });

  beforeEach(() => {
    keycloak.login.mockClear();
    keycloak.logout.mockClear();
    keycloak.tokenParsed = undefined;
    keycloakEvent.set({ type: KeycloakEventType.Ready, args: true });
    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakEvent },
      ],
    });
  });

  it('uses stable token claim fallbacks for the account name', () => {
    const auth = TestBed.inject(AuthStateService);

    expect(auth.userName()).toBe('Signed in');

    keycloak.tokenParsed = {
      email: 'operator@example.test',
      preferred_username: 'operator',
      name: 'PegelHub Operator',
    };
    keycloakEvent.set({ type: KeycloakEventType.AuthSuccess });
    expect(auth.userName()).toBe('PegelHub Operator');

    keycloak.tokenParsed = { email: 'operator@example.test', preferred_username: 'operator' };
    keycloakEvent.set({ type: KeycloakEventType.AuthRefreshSuccess });
    expect(auth.userName()).toBe('operator');

    keycloak.tokenParsed = { email: 'operator@example.test' };
    keycloakEvent.set({ type: KeycloakEventType.AuthRefreshSuccess });
    expect(auth.userName()).toBe('operator@example.test');
  });

  it('uses application URLs for login and logout redirects', async () => {
    const auth = TestBed.inject(AuthStateService);

    await auth.login('/overview/series-water-level');
    await auth.logout();

    expect(keycloak.login).toHaveBeenCalledWith({
      redirectUri: new URL('/overview/series-water-level', window.location.origin).toString(),
    });
    expect(keycloak.logout).toHaveBeenCalledWith({
      redirectUri: `${window.location.origin}/`,
    });
  });
});
