import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MemoryStorage } from '../../../testing/memory-storage';
import { initializeDocumentTheme, ThemeService } from './theme.service';

describe('ThemeService', () => {
  let storage: Storage;

  beforeEach(() => {
    storage = new MemoryStorage();
    Object.defineProperty(window, 'localStorage', { configurable: true, value: storage });
    document.documentElement.classList.remove('ph-dark');
    document.documentElement.style.removeProperty('color-scheme');
  });

  it('uses the operating-system preference when no choice is stored', () => {
    stubMatchMedia(true);

    expect(initializeDocumentTheme(document)).toBe('dark');
    expect(document.documentElement.classList.contains('ph-dark')).toBe(true);
  });

  it('prefers a stored user choice over the operating-system preference', () => {
    storage.setItem('pegelhub.theme', 'light');
    stubMatchMedia(true);

    expect(initializeDocumentTheme(document)).toBe('light');
    expect(document.documentElement.classList.contains('ph-dark')).toBe(false);
  });

  it('toggles and persists the selected mode', () => {
    stubMatchMedia(false);
    const service = TestBed.inject(ThemeService);

    service.toggle();

    expect(service.mode()).toBe('dark');
    expect(storage.getItem('pegelhub.theme')).toBe('dark');
    expect(document.documentElement.classList.contains('ph-dark')).toBe(true);
  });
});

function stubMatchMedia(matches: boolean): void {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn().mockReturnValue({
      matches,
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }),
  });
}
