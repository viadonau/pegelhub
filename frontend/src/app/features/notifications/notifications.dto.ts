export interface DestinationConfig {
  name: string;
  enabled: boolean;
  transport: 'SMTP' | 'SNMP';
  credentialRef: string;
  mail: { from: string; recipients: string[]; signature: string | null } | null;
  snmp: {
    host: string;
    port: number;
    version: 'v2c' | 'v3';
    trapOid: string;
    messageOid: string;
    enterpriseOid: string | null;
  } | null;
}
export interface Destination {
  id: string;
  configuration: DestinationConfig;
}
export interface Delivery {
  id: string;
  requestId: string;
  sourceId: string | null;
  destinationId: string;
  route: DestinationConfig;
  subject: string;
  body: string;
  state: 'PENDING' | 'SENDING' | 'ACCEPTED' | 'FAILED' | 'CANCELLED';
  attempts: number;
  createdAt: string;
  nextAttemptAt: string;
  completedAt: string | null;
  lastError: string | null;
}
export interface NotificationStatus {
  enabled: boolean;
  retentionDays: number;
  credentials: { name: string; kind: string; available: boolean }[];
}
