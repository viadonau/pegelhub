import { DOCUMENT } from '@angular/common';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'pegelhub.theme';
const DARK_MODE_QUERY = '(prefers-color-scheme: dark)';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly window = this.document.defaultView;
  private readonly storage = getStorage(this.window);
  private readonly mediaQuery = this.window?.matchMedia?.(DARK_MODE_QUERY);
  private readonly selectedMode = signal(initializeDocumentTheme(this.document));
  private followsSystemPreference = readStoredMode(this.storage) === null;

  readonly mode = this.selectedMode.asReadonly();
  readonly isDark = computed(() => this.mode() === 'dark');
  readonly toggleLabel = computed(() =>
    this.isDark() ? 'Helles Erscheinungsbild verwenden' : 'Dunkles Erscheinungsbild verwenden',
  );
  readonly toggleIcon = computed(() => (this.isDark() ? 'pi pi-sun' : 'pi pi-moon'));

  constructor() {
    const onSystemPreferenceChange = (event: MediaQueryListEvent): void => {
      if (this.followsSystemPreference) {
        this.setMode(event.matches ? 'dark' : 'light', false);
      }
    };

    this.mediaQuery?.addEventListener('change', onSystemPreferenceChange);
    inject(DestroyRef).onDestroy(() =>
      this.mediaQuery?.removeEventListener('change', onSystemPreferenceChange),
    );
  }

  toggle(): void {
    this.followsSystemPreference = false;
    this.setMode(this.isDark() ? 'light' : 'dark', true);
  }

  private setMode(mode: ThemeMode, persist: boolean): void {
    this.selectedMode.set(mode);
    applyDocumentTheme(this.document, mode);

    if (persist) {
      writeStoredMode(this.storage, mode);
    }
  }
}

export function initializeDocumentTheme(document: Document): ThemeMode {
  const window = document.defaultView;
  const storedMode = readStoredMode(getStorage(window));
  const mode = storedMode ?? (window?.matchMedia?.(DARK_MODE_QUERY).matches ? 'dark' : 'light');

  applyDocumentTheme(document, mode);
  return mode;
}

function applyDocumentTheme(document: Document, mode: ThemeMode): void {
  const isDark = mode === 'dark';

  document.documentElement.classList.toggle('ph-dark', isDark);
  document.documentElement.style.colorScheme = mode;
  document
    .querySelector<HTMLMetaElement>('meta[name="theme-color"]')
    ?.setAttribute('content', isDark ? '#0b1820' : '#ffffff');
}

function getStorage(window: Window | null | undefined): Storage | undefined {
  try {
    return window?.localStorage;
  } catch {
    return undefined;
  }
}

function readStoredMode(storage: Storage | undefined): ThemeMode | null {
  try {
    const value = storage?.getItem(STORAGE_KEY);
    return value === 'light' || value === 'dark' ? value : null;
  } catch {
    return null;
  }
}

function writeStoredMode(storage: Storage | undefined, mode: ThemeMode): void {
  try {
    storage?.setItem(STORAGE_KEY, mode);
  } catch {
    // A blocked storage API should not prevent an in-session theme change.
  }
}
