// Tipos que espelham a resposta de GET /api/health-check (veja docs/api-saude.md).

export type Severity = 'INFO' | 'WARNING' | 'CRITICAL'
export type AlertKind = 'NORMAL' | 'PREVENTIVE' | 'UNAVAILABLE'

export interface HealthAlert {
  id: string
  component: string
  severity: Severity
  kind: AlertKind
  title: string
  description: string
  /** Valor já formatado com unidade; null = sem leitura. */
  observedValue: string | null
  /** Limite aplicado, com unidade; null = não se aplica. */
  threshold: string | null
  timestamp: string
}

export interface HealthThresholds {
  cpuTempWarningC: number
  cpuTempCriticalC: number
  diskFreeWarningPercent: number
  diskFreeCriticalPercent: number
  ramUsedWarningPercent: number
}

export interface HealthReport {
  collectedAt: string
  source: string
  overall: Severity
  infoCount: number
  warningCount: number
  criticalCount: number
  thresholds: HealthThresholds
  thresholdsSource: string
  alerts: HealthAlert[]
}
