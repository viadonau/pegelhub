export interface QualityRules {
  range: { min: number; max: number } | null;
  jumpThreshold: number | null;
  frozenSeconds: number | null;
  maxDeviation: number | null;
}

export interface ProfileConfig {
  name: string;
  enabled: boolean;
  sources: string[];
  rules: QualityRules;
  lookbackSeconds: number;
  intervalSeconds: number;
  destinations: string[];
  output: string | null;
}

export interface QualityProfile {
  id: string;
  configuration: ProfileConfig;
  nextRunAt: string;
  currentRunId: string | null;
  runRequested: boolean;
}

export interface QualityRun {
  id: string;
  profileId: string;
  configuration: ProfileConfig;
  state: 'RUNNING' | 'PASSED' | 'FINDINGS' | 'INCOMPLETE' | 'ERROR' | 'INTERRUPTED';
  startedAt: string;
  completedAt: string | null;
  findingCount: number;
  outputState: 'NOT_CONFIGURED' | 'SUPPRESSED' | 'PENDING' | 'WRITTEN' | 'FAILED' | 'INTERRUPTED';
  error: string | null;
}

export interface Finding {
  rule: 'RANGE' | 'JUMP' | 'FROZEN' | 'DEVIATION' | 'DATA';
  severity: 'WARNING' | 'FAILED';
  timeSeriesIds: string[];
  observedAt: string | null;
  message: string;
  evidence: Record<string, number>;
}

export interface QualityStatus {
  enabled: boolean;
  maximumPoints: number;
  timeoutSeconds: number;
  retentionDays: number;
}
export interface CatalogSeries {
  id: string;
  measuringPointId: string;
  observedProperty: string;
  unit: string;
  status: 'active' | 'inactive';
  sourceAssignment: {
    connectorId: string | null;
    representation: string;
    internalProducerId?: string | null;
  } | null;
}
