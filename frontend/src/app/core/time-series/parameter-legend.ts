export type StationParameterCode = 'W' | 'WT' | 'Q';

/**
 * Display metadata for the station parameter codes used in the operator
 * catalogue (see docs/operator-station-metadata.md).
 *
 * The codes themselves (`W`, `WT`, `Q`) are the operator vocabulary and stay
 * verbatim in the API contract; labels are used for the operator-facing view.
 */
export interface StationParameterMeta {
  code: StationParameterCode;
  label: string;
  unit: string;
}

export const STATION_PARAMETER_LEGEND: Record<StationParameterCode, StationParameterMeta> = {
  W: { code: 'W', label: 'Wasserstand', unit: 'cm' },
  WT: { code: 'WT', label: 'Wassertemperatur', unit: '°C' },
  Q: { code: 'Q', label: 'Abfluss', unit: 'm³/s' },
};

export function describeParameter(code: string): StationParameterMeta | undefined {
  return STATION_PARAMETER_LEGEND[code as StationParameterCode];
}

const PARAMETER_CODE_PATTERNS: Array<{ code: StationParameterCode; pattern: RegExp }> = [
  { code: 'WT', pattern: /(water[-_\s]?temperature|wassertemperatur|temperature)/i },
  { code: 'Q', pattern: /(discharge|abfluss|durchfluss)/i },
  { code: 'W', pattern: /(water[-_\s]?level|wasserstand|gauge[-_\s]?level|level)/i },
];

export function detectParameterCode(value: string): StationParameterCode | null {
  const normalized = value.trim().toUpperCase();

  if (normalized === 'W' || normalized === 'WT' || normalized === 'Q') {
    return normalized;
  }

  for (const entry of PARAMETER_CODE_PATTERNS) {
    if (entry.pattern.test(value)) return entry.code;
  }

  return null;
}

export function humanizeObservedProperty(value: string): string {
  const spaced = value
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .trim();

  if (!spaced) {
    return value;
  }

  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}

export function observedPropertyLabel(value: string): string {
  const code = detectParameterCode(value);

  return code
    ? (describeParameter(code)?.label ?? humanizeObservedProperty(value))
    : humanizeObservedProperty(value);
}
