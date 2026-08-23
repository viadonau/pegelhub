import { afterEach, describe, expect, it, vi } from 'vitest';

import { loadRuntimeConfig, RuntimeConfigError } from './runtime-config';

const VALID_CONFIG = {
  apiBaseUrl: '/api/v1',
  keycloak: {
    url: 'https://auth.staging.test',
    realm: 'pegelhub',
    clientId: 'pegelhub-frontend',
  },
};

describe('runtime configuration', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('loads the complete deployment contract', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(VALID_CONFIG), {
          headers: { 'Content-Type': 'application/json' },
          status: 200,
        }),
      ),
    );

    await expect(loadRuntimeConfig()).resolves.toEqual(VALID_CONFIG);
    expect(fetch).toHaveBeenCalledWith('/assets/config.json', { cache: 'no-store' });
  });

  it.each([
    ['a network failure', vi.fn().mockRejectedValue(new Error('offline')), 'offline'],
    [
      'a non-success response',
      vi.fn().mockResolvedValue(new Response(null, { status: 503, statusText: 'Unavailable' })),
      '503 Unavailable',
    ],
    [
      'invalid JSON',
      vi.fn().mockResolvedValue(new Response('{', { status: 200 })),
      'is not valid JSON',
    ],
  ])('reports %s with deployment context', async (_case, fetchMock, message) => {
    vi.stubGlobal('fetch', fetchMock);

    await expect(loadRuntimeConfig()).rejects.toSatisfy(
      (error: unknown) =>
        error instanceof RuntimeConfigError &&
        error.message.includes('/assets/config.json') &&
        error.message.includes(message),
    );
  });

  it.each([
    ['non-object JSON', 42, 'apiBaseUrl, keycloak.url, keycloak.realm, keycloak.clientId'],
    [
      'blank and missing fields',
      {
        apiBaseUrl: '   ',
        keycloak: { url: VALID_CONFIG.keycloak.url, realm: '' },
      },
      'apiBaseUrl, keycloak.realm, keycloak.clientId',
    ],
  ])('rejects %s', async (_case, config, missingFields) => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(config), { status: 200 })),
    );

    await expect(loadRuntimeConfig()).rejects.toEqual(
      new RuntimeConfigError(
        `Runtime config is missing required PegelHub settings: ${missingFields}.`,
      ),
    );
  });
});
