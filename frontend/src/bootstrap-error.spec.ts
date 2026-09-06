import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { renderBootstrapError } from './bootstrap-error';

describe('bootstrap error renderer', () => {
  let originalBody: string;

  beforeEach(() => {
    originalBody = document.body.innerHTML;
  });

  afterEach(() => {
    document.body.innerHTML = originalBody;
    document.body.style.removeProperty('margin');
  });

  it('renders a labeled startup error with the known message', () => {
    renderBootstrapError(new Error('Keycloak origin is not allowed'));

    const main = document.querySelector('main');
    const panel = main?.querySelector('section');
    const heading = main?.querySelector('h1');

    expect(main).not.toBeNull();
    expect(panel?.getAttribute('aria-labelledby')).toBe(heading?.id);
    expect(heading?.textContent).toBe('Frontend could not start');
    expect(main?.querySelector('pre')?.textContent).toBe('Keycloak origin is not allowed');
  });

  it('safely renders an unknown error without stringifying arbitrary input', () => {
    renderBootstrapError({ secret: 'do not expose' });

    expect(document.querySelector('main pre')?.textContent).toBe('Unknown startup error.');
    expect(document.body.textContent).not.toContain('do not expose');
  });
});
