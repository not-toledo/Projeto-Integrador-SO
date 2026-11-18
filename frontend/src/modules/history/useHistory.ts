import { useCallback, useEffect, useState } from 'react'
import { fetchHistory } from '../../services/historyService'
import type { HistoryResponse } from './types'

interface HistoryState {
  history: HistoryResponse | null
  loading: boolean
  error: string | null
  refresh: () => void
}

/** Resposta (ou erro) de uma consulta identificada pelo período e pelo número da atualização. */
interface Outcome {
  days: number
  request: number
  history?: HistoryResponse
  error?: string
}

/**
 * Carrega o histórico do período; trocar o período cancela a consulta anterior. "Carregando" é derivado: a última
 * resposta ainda não corresponde ao período e à atualização pedidos. O histórico anterior continua visível enquanto
 * isso.
 */
export function useHistory(days: number): HistoryState {
  const [request, setRequest] = useState(0)
  const [outcome, setOutcome] = useState<Outcome | null>(null)
  const [last, setLast] = useState<HistoryResponse | null>(null)

  useEffect(() => {
    const abort = new AbortController()
    fetchHistory(days, abort.signal)
      .then((history) => {
        if (abort.signal.aborted) return
        setLast(history)
        setOutcome({ days, request, history })
      })
      .catch((reason: unknown) => {
        if (abort.signal.aborted) return
        setOutcome({ days, request, error: reason instanceof Error ? reason.message : 'Falha ao obter o histórico.' })
      })
    return () => abort.abort()
  }, [days, request])

  const refresh = useCallback(() => setRequest((n) => n + 1), [])
  const current = outcome !== null && outcome.days === days && outcome.request === request
  return {
    history: last,
    loading: !current,
    error: current ? outcome.error ?? null : null,
    refresh,
  }
}
