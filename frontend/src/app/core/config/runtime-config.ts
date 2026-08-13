import { InjectionToken } from '@angular/core';

export interface RuntimeConfig {
  apiBaseUrl: string;
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
}

const CONFIG_PATH = '/assets/config.json';
const REQUIRED_FIELDS = [
  'apiBaseUrl',
  'keycloak.url',
  'keycloak.realm',
  'keycloak.clientId',
] as const;

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('PegelHub runtime config');

export class RuntimeConfigError extends Error {
  override readonly name = 'RuntimeConfigError';
}

export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  let response: Response;

  try {
    response = await fetch(CONFIG_PATH, { cache: 'no-store' });
  } catch (error) {
    throw new RuntimeConfigError(
      `Could not load runtime config from ${CONFIG_PATH}: ${errorMessage(error)}`,
    );
  }

  if (!response.ok) {
    throw new RuntimeConfigError(
      `Could not load runtime config from ${CONFIG_PATH}: ${response.status} ${response.statusText}`,
    );
  }

  let config: unknown;

  try {
    config = await response.json();
  } catch (error) {
    throw new RuntimeConfigError(
      `Runtime config at ${CONFIG_PATH} is not valid JSON: ${errorMessage(error)}`,
    );
  }

  const missingFields = requiredRuntimeConfigFields(config);

  if (missingFields.length > 0) {
    throw new RuntimeConfigError(
      `Runtime config is missing required PegelHub settings: ${missingFields.join(', ')}.`,
    );
  }

  return config as RuntimeConfig;
}

function requiredRuntimeConfigFields(config: unknown): string[] {
  if (!isRecord(config)) {
    return [...REQUIRED_FIELDS];
  }

  const keycloak = isRecord(config['keycloak']) ? config['keycloak'] : null;

  return REQUIRED_FIELDS.filter((field) => {
    const value = field.startsWith('keycloak.')
      ? keycloak?.[field.replace('keycloak.', '')]
      : config[field];

    return typeof value !== 'string' || value.trim() === '';
  });
}

function errorMessage(error: unknown): string {
  return error instanceof Error && error.message ? error.message : 'Unknown error';
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
