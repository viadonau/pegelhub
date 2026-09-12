import { HttpErrorResponse } from '@angular/common/http';

const labels: Record<string, string> = {
  RUNNING: 'Läuft',
  PASSED: 'Unauffällig',
  FINDINGS: 'Befunde',
  INCOMPLETE: 'Unvollständig',
  ERROR: 'Fehler',
  INTERRUPTED: 'Unterbrochen',
  NOT_CONFIGURED: 'Kein Ausgabeziel',
  SUPPRESSED: 'Keine Ausgabe',
  PENDING: 'Ausstehend',
  WRITTEN: 'Geschrieben',
  FAILED: 'Fehlgeschlagen',
  SENDING: 'Wird gesendet',
  ACCEPTED: 'An Transport übergeben',
  CANCELLED: 'Abgebrochen',
  WARNING: 'Warnung',
  RANGE: 'Wertebereich',
  JUMP: 'Sprung',
  FROZEN: 'Stillstand',
  DEVIATION: 'Abweichung',
  DATA: 'Daten fehlen',
};

export function operationLabel(value: string): string {
  return labels[value] ?? value;
}

export function operationSeverity(value: string): 'success' | 'danger' | 'warn' | 'secondary' {
  if (['PASSED', 'WRITTEN', 'ACCEPTED'].includes(value)) {
    return 'success';
  }
  if (['FAILED', 'ERROR'].includes(value)) {
    return 'danger';
  }
  if (['FINDINGS', 'INCOMPLETE', 'WARNING', 'INTERRUPTED'].includes(value)) {
    return 'warn';
  }

  return 'secondary';
}

export function operationError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 409) {
    return 'Die Konfiguration steht im Konflikt oder ein Lauf ist noch aktiv.';
  }
  if (error instanceof HttpErrorResponse && error.status === 400) {
    return 'Die Konfiguration ist ungültig. Bitte Eingaben, Quellen und Zielzuordnung prüfen.';
  }

  return 'Die Anfrage ist fehlgeschlagen. Bitte erneut versuchen.';
}

export function downloadYaml(document: string, name: string): void {
  const url = URL.createObjectURL(new Blob([document], { type: 'application/yaml' }));
  const anchor = window.document.createElement('a');
  anchor.href = url;
  anchor.download = `${name}.yaml`;

  anchor.click();
  URL.revokeObjectURL(url);
}

export async function readConfiguration(file: File): Promise<string> {
  if (file.size > 65_536) {
    throw new Error('Configuration too large');
  }

  return file.text();
}
