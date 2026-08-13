import { describe, expect, it } from 'vitest';

import { detectParameterCode, observedPropertyLabel } from './parameter-legend';

describe('parameter legend', () => {
  it('recognizes both operator codes and descriptive backend values', () => {
    expect(detectParameterCode('W')).toBe('W');
    expect(detectParameterCode('WT')).toBe('WT');
    expect(detectParameterCode('Q')).toBe('Q');
    expect(detectParameterCode('water-temperature')).toBe('WT');
  });

  it('uses the operator label for a literal parameter code', () => {
    expect(observedPropertyLabel('W')).toBe('Wasserstand');
  });
});
