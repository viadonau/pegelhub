const PARAMETERS: Record<string, { label: string; unit: string }> = {
  'water-level': { label: 'Wasserstand', unit: 'cm' },
  'water-temperature': { label: 'Wassertemperatur', unit: '°C' },
  discharge: { label: 'Abfluss', unit: 'm³/s' },
};

export function observedPropertyLabel(value: string): string {
  return PARAMETERS[value]?.label ?? value;
}

export function observedPropertyUnit(value: string, canonicalUnit: string): string {
  return PARAMETERS[value]?.unit ?? canonicalUnit;
}
