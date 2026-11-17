import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchHealthReport } from '../../services/healthService'
import type { HealthReport } from './types'

export interface HealthCheckState {
  /** Último relatório recebido com sucesso (mantido se uma atualização falhar). */
  report: HealthReport | null
  loading: boolean
  /** Mensagem do último erro de comunicação, ou null. */
  error: string | null
  refresh: () => void
}

/** Carrega o Health Check ao montar e a cada atualização manual; nunca dispara duas requisições ao mesmo tempo. */
export function useHealthCheck(): HealthCheckState {
  const [report, setReport] = useState<HealthReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const controller = useRef<AbortController | null>(null)
  const inFlight = useRef(false)

  const load = useCallback(() => {
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    const abort = new AbortController()
    controller.current = abort
    setLoading(true)
    setError(null)

    fetchHealthReport(abort.signal)
      .then((result) => {
        if (!abort.signal.aborted) {
          setReport(result)
        }
      })
      .catch((reason: unknown) => {
        if (!abort.signal.aborted) {
          setError(reason instanceof Error ? reason.message : 'Falha desconhecida ao obter o Health Check.')
        }
      })
      .finally(() => {
        // Uma requisição abortada não pode liberar a trava de uma requisição mais nova.
        if (controller.current === abort) {
          inFlight.current = false
        }
        if (!abort.signal.aborted) {
          setLoading(false)
        }
      })
  }, [])

  useEffect(() => {
    load()
    return () => {
      controller.current?.abort()
      inFlight.current = false
    }
  }, [load])

  return { report, loading, error, refresh: load }
}
