import {
  MonitoringMeasuringPointDto,
  MonitoringTimeSeriesDetailDto,
} from '../../../core/api/monitoring.dto';

export interface WaterLevelReference {
  label: string;
  value: number;
  tone: 'lower' | 'upper';
}

export function waterLevelChartReferences(
  measuringPoint: MonitoringMeasuringPointDto,
  timeSeries: MonitoringTimeSeriesDetailDto,
): WaterLevelReference[] {
  if (timeSeries.observedProperty !== 'water-level') {
    return [];
  }

  const referenceYear = measuringPoint.referenceYear;

  return [
    {
      label: waterLevelReferenceLabel('RNW', referenceYear),
      value: measuringPoint.rnw,
      tone: 'lower' as const,
    },
    {
      label: waterLevelReferenceLabel('HSW', referenceYear),
      value: measuringPoint.hsw,
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
