import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import {
  AutoRefreshTokenService,
  createInterceptorCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  IncludeBearerTokenCondition,
  includeBearerTokenInterceptor,
  provideKeycloak,
  UserActivityService,
  withAutoRefreshToken,
} from 'keycloak-angular';

import { routes } from './app.routes';
import { RUNTIME_CONFIG, RuntimeConfig } from './core/config/runtime-config';
import { authErrorInterceptor } from './core/http/auth-error.interceptor';
import { ViadonauPreset } from './core/theme/viadonau.preset';

export const KEYCLOAK_SESSION_IDLE_TIMEOUT_MS = 30 * 60 * 1000;

export function createAppConfig(runtimeConfig: RuntimeConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideZonelessChangeDetection(),
      provideRouter(routes, withComponentInputBinding()),
      provideHttpClient(
        withFetch(),
        withInterceptors([includeBearerTokenInterceptor, authErrorInterceptor]),
      ),
      providePrimeNG({
        theme: {
          preset: ViadonauPreset,
          options: {
            darkModeSelector: '.ph-dark',
          },
        },
        ripple: true,
      }),
      {
        provide: RUNTIME_CONFIG,
        useValue: runtimeConfig,
      },
      {
        provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
        useValue: [
          createInterceptorCondition<IncludeBearerTokenCondition>({
            urlPattern: coreApiUrlPattern(runtimeConfig.apiBaseUrl),
            bearerPrefix: 'Bearer',
          }),
        ],
      },
      provideKeycloak({
        config: {
          url: runtimeConfig.keycloak.url,
          realm: runtimeConfig.keycloak.realm,
          clientId: runtimeConfig.keycloak.clientId,
        },
        initOptions: {
          onLoad: 'login-required',
          pkceMethod: 'S256',
          checkLoginIframe: shouldCheckLoginIframe(runtimeConfig.keycloak.url),
        },
        features: [
          withAutoRefreshToken({
            onInactivityTimeout: 'login',
            sessionTimeout: KEYCLOAK_SESSION_IDLE_TIMEOUT_MS,
          }),
        ],
        providers: [AutoRefreshTokenService, UserActivityService],
      }),
    ],
  };
}

function shouldCheckLoginIframe(keycloakUrl: string): boolean {
  const browserProtocol = globalThis.location?.protocol;
  const keycloakProtocol = new URL(keycloakUrl).protocol;

  return browserProtocol === 'https:' && keycloakProtocol === 'https:';
}

function coreApiUrlPattern(apiBaseUrl: string): RegExp {
  const normalized = apiBaseUrl.replace(/\/$/, '');

  if (/^https?:\/\//i.test(normalized)) {
    return new RegExp(`^${escapeRegExp(normalized)}(?:/.*)?$`, 'i');
  }

  return new RegExp(
    `^${escapeRegExp(normalized.startsWith('/') ? normalized : `/${normalized}`)}(?:/.*)?$`,
    'i',
  );
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
