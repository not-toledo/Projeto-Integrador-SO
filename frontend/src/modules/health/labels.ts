import type { AlertKind, HealthAlert, Severity } from './types'

/** Nomes padronizados das severidades. */
export const SEVERITY_LABEL: Record<Severity, string> = {
  INFO: 'Informação',
  WARNING: 'Atenção',
  CRITICAL: 'Crítico',
}

/** Natureza do alerta: distingue indicador preventivo de falha comprovada. */
export const KIND_LABEL: Record<AlertKind, string> = {
  NORMAL: 'Normal',
  PREVENTIVE: 'Alerta preventivo — não comprova defeito',
  UNAVAILABLE: 'Dado indisponível — não é falha física',
}

/** Resumo geral em linguagem simples, sem afirmar defeito. */
export function overallMessage(overall: Severity, criticalCount: number, warningCount: number): string {
  if (overall === 'CRITICAL') {
    return `${criticalCount} indicador(es) em nível crítico. São alertas preventivos: verifique os detalhes abaixo.`
  }
  if (overall === 'WARNING') {
    return `${warningCount} indicador(es) pedem atenção. Nenhum está em nível crítico.`
  }
  return 'Nenhum indicador avaliado passou dos limites configurados.'
}

/** Ordem para exibição: mais grave primeiro (não depende da ordem enviada pela API). */
const RANK: Record<Severity, number> = { CRITICAL: 0, WARNING: 1, INFO: 2 }

export function sortAlerts(alerts: HealthAlert[]): HealthAlert[] {
  return [...alerts].sort((a, b) => RANK[a.severity] - RANK[b.severity])
}
