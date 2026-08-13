import { computed, inject, Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  readonly userName = computed(() => {
    this.keycloakEvent();
    const token = this.keycloak.tokenParsed;
    return token?.['name'] ?? token?.['preferred_username'] ?? token?.['email'] ?? 'Signed in';
  });

  login(returnUrl = '/'): Promise<void> {
    return this.keycloak.login({
      redirectUri: new URL(returnUrl, window.location.origin).toString(),
    });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({
      redirectUri: `${window.location.origin}/`,
    });
  }
}
