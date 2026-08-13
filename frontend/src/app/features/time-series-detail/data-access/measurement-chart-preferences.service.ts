import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'pegelhub.chart.referenceLevels';

@Injectable({ providedIn: 'root' })
export class MeasurementChartPreferences {
  private readonly storage = getStorage(inject(DOCUMENT).defaultView);
  private readonly referenceLevelsVisible = signal(readVisibility(this.storage));

  readonly showReferenceLevels = this.referenceLevelsVisible.asReadonly();

  setReferenceLevelsVisible(visible: boolean): void {
    this.referenceLevelsVisible.set(visible);
    writeVisibility(this.storage, visible);
  }
}

function getStorage(window: Window | null | undefined): Storage | undefined {
  try {
    return window?.localStorage;
  } catch {
    return undefined;
  }
}

function readVisibility(storage: Storage | undefined): boolean {
  try {
    return storage?.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function writeVisibility(storage: Storage | undefined, visible: boolean): void {
  try {
    storage?.setItem(STORAGE_KEY, String(visible));
  } catch {
    // The chart remains usable when browser storage is unavailable.
  }
}
