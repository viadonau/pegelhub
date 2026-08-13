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

  it('rejects runtime config without the frontend client identifier', async () => {
    const config = {
      ...VALID_CONFIG,
      keycloak: {
        ...VALID_CONFIG.keycloak,
        clientId: '',
      },
    };
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(config), { status: 200 })),
    );

    await expect(loadRuntimeConfig()).rejects.toEqual(
      new RuntimeConfigError(
        'Runtime config is missing required PegelHub settings: keycloak.clientId.',
      ),
    );
  });
});
