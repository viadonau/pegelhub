import { describe, expect, it } from 'vitest';

import { observedPropertyLabel } from './parameter-legend';

describe('parameter legend', () => {
  it('maps canonical properties exactly', () => {
    expect(observedPropertyLabel('water-level')).toBe('Wasserstand');
    expect(observedPropertyLabel('water-temperature')).toBe('Wassertemperatur');
    expect(observedPropertyLabel('discharge')).toBe('Abfluss');
  });

  it('leaves unknown properties unchanged', () => {
    expect(observedPropertyLabel('air_temperature')).toBe('air_temperature');
  });
});
