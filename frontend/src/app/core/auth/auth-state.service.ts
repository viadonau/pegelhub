import { computed, inject, Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  readonly isAdmin = computed(() => this.hasUserRole('system:admin'));
  readonly canMonitor = computed(
    () =>
      this.isAdmin() || (this.hasUserRole('metadata:read') && this.hasUserRole('measurement:read')),
  );

  private hasUserRole(role: string): boolean {
    this.keycloakEvent();
    const token = this.keycloak.tokenParsed;
    return (
      token?.['pegelhub_actor_type'] === 'USER' &&
      (token.resource_access?.['pegelhub-core-api']?.roles.includes(role) ?? false)
    );
  }

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
