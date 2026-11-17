// Dados FICTÍCIOS usados apenas nos testes automatizados. Nunca são importados pela aplicação.
import type { HealthAlert, HealthReport, Severity } from './types'

const NOW = '2026-01-01T12:00:00Z'

export function makeAlert(overrides: Partial<HealthAlert> = {}): HealthAlert {
  return {
    id: 'memory:usage',
    component: 'Memória RAM',
    severity: 'INFO',
    kind: 'NORMAL',
    title: 'Uso de RAM normal',
    description: 'O uso de memória está abaixo do limite de aviso configurado.',
    observedValue: '50,0%',
    threshold: 'Aviso a partir de 85,0%',
    timestamp: NOW,
    ...overrides,
  }
}

export function makeReport(alerts: HealthAlert[], overrides: Partial<HealthReport> = {}): HealthReport {
  const count = (s: Severity) => alerts.filter((a) => a.severity === s).length
  const overall: Severity = count('CRITICAL') > 0 ? 'CRITICAL' : count('WARNING') > 0 ? 'WARNING' : 'INFO'
  return {
    collectedAt: NOW,
    source: 'Origem de teste',
    overall,
    infoCount: count('INFO'),
    warningCount: count('WARNING'),
    criticalCount: count('CRITICAL'),
    thresholds: {
      cpuTempWarningC: 80,
      cpuTempCriticalC: 95,
      diskFreeWarningPercent: 15,
      diskFreeCriticalPercent: 5,
      ramUsedWarningPercent: 85,
    },
    thresholdsSource: 'Limites genéricos de teste',
    alerts,
    ...overrides,
  }
}

export const normalAlerts: HealthAlert[] = [
  makeAlert({ id: 'temperature:cpu', component: 'Temperatura', title: 'Temperatura da CPU normal', observedValue: '45,0 °C' }),
  makeAlert(),
]

export const attentionAlerts: HealthAlert[] = [
  ...normalAlerts,
  makeAlert({
    id: 'disk-space:C:',
    component: 'Disco',
    severity: 'WARNING',
    kind: 'PREVENTIVE',
    title: 'Espaço livre baixo em C:',
    observedValue: '10,0 GiB livres de 100,0 GiB (10,0% livre)',
    threshold: 'Atenção abaixo de 15,0% livre',
  }),
]

export const criticalAlerts: HealthAlert[] = [
  ...attentionAlerts.filter((a) => a.id !== 'temperature:cpu'),
  makeAlert({
    id: 'temperature:cpu',
    component: 'Temperatura',
    severity: 'CRITICAL',
    kind: 'PREVENTIVE',
    title: 'Temperatura da CPU muito alta',
    observedValue: '97,0 °C',
    threshold: 'Crítico a partir de 95,0 °C',
  }),
]

export const noSensorAlerts: HealthAlert[] = [
  makeAlert({
    id: 'temperature:cpu',
    component: 'Temperatura',
    kind: 'UNAVAILABLE',
    title: 'Temperatura da CPU não disponível',
    description: 'O sistema não informou uma leitura de temperatura.',
    observedValue: null,
    threshold: null,
  }),
  makeAlert(),
]
