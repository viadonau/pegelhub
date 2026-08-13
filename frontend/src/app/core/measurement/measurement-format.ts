export const UI_LOCALE = 'de-AT';
const dateTimeFormatter = new Intl.DateTimeFormat(UI_LOCALE, {
  dateStyle: 'medium',
  timeStyle: 'short',
});
const numberFormatter = new Intl.NumberFormat(UI_LOCALE, {
  maximumFractionDigits: 3,
});

export function formatMeasurementNumber(value: number): string {
  return numberFormatter.format(value);
}

export function formatMeasurementValue(value: number, unit: string | null): string {
  const formatted = formatMeasurementNumber(value);

  return unit ? `${formatted} ${unit}` : formatted;
}

export function formatMeasurementTimestamp(value: string): string {
  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? value : dateTimeFormatter.format(date);
}

export function formatRelativeMeasurementAge(value: string, now: Date = new Date()): string | null {
  const observedAt = new Date(value);
  const ageMilliseconds = now.getTime() - observedAt.getTime();

  if (Number.isNaN(ageMilliseconds) || ageMilliseconds < 0) {
    return null;
  }

  const ageMinutes = Math.floor(ageMilliseconds / 60_000);

  if (ageMinutes < 1) {
    return 'gerade eben';
  }

  if (ageMinutes < 60) {
    return `vor ${ageMinutes} Min.`;
  }

  const ageHours = Math.floor(ageMinutes / 60);

  if (ageHours < 24) {
    return `vor ${ageHours} Std.`;
  }

  const ageDays = Math.floor(ageHours / 24);

  if (ageDays === 1) {
    return 'vor 1 Tag';
  }

  return ageDays < 7 ? `vor ${ageDays} Tagen` : null;
}
