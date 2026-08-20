import { MonitoringMeasuringPointDto } from '../../../core/api/monitoring.dto';

export interface WaterLevelReference {
  label: string;
  value: number;
  tone: 'lower' | 'upper';
}

export function waterLevelChartReferences(
  point: MonitoringMeasuringPointDto,
): WaterLevelReference[] {
  const references = point.waterLevelReferences;
  if (!references) return [];

  return [
    {
      label: waterLevelReferenceLabel('RNW', references.referenceSetYear),
      value: references.rnwCm,
      tone: 'lower' as const,
    },
    {
      label: waterLevelReferenceLabel('HSW', references.referenceSetYear),
      value: references.hswCm,
      tone: 'upper' as const,
    },
  ].filter(isPresentReference);
}

export function waterLevelReferenceLabel(prefix: string, year: number | null | undefined): string {
  return year === undefined || year === null ? prefix : `${prefix} ${year}`;
}

function isPresentReference(reference: {
  label: string;
  value: number | null | undefined;
  tone: 'lower' | 'upper';
}): reference is WaterLevelReference {
  return typeof reference.value === 'number' && Number.isFinite(reference.value);
}
