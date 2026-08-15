const LABELS: Record<string, string> = {
  'water-level': 'Wasserstand',
  'water-temperature': 'Wassertemperatur',
  discharge: 'Abfluss',
};

export function observedPropertyLabel(value: string): string {
  return LABELS[value] ?? value;
}
