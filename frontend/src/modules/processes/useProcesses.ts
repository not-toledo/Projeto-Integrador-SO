import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchProcesses } from '../../services/processService'
import type { ProcessesResponse } from './types'

/** Espera entre o fim de uma leitura e o início da próxima (cada leitura já leva cerca de 1 s no backend). */
export const AUTO_REFRESH_MS = 5000

export interface ProcessesState {
  data: ProcessesResponse | null
  loading: boolean
  error: string | null
  auto: boolean
  setAuto: (on: boolean) => void
  refresh: () => void
}

/**
 * Lê os processos ao abrir e, com a atualização automática ligada, de novo 5 s depois de cada leitura terminar
 * (nunca duas ao mesmo tempo). Uma falha mantém a última lista recebida.
 */
export function useProcesses(): ProcessesState {
  const [data, setData] = useState<ProcessesResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [auto, setAutoState] = useState(true)
  const [tick, setTick] = useState(0)
  const autoRef = useRef(true)
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

  // "Lendo" é marcado por quem pede a leitura (botão, timer ou religar o automático), não dentro do efeito.
  const next = useCallback(() => {
    setLoading(true)
    setTick((t) => t + 1)
  }, [])

  const setAuto = useCallback((on: boolean) => {
    autoRef.current = on
    setAutoState(on)
    clearTimeout(timer.current)
    if (on) next()
  }, [next])

  useEffect(() => {
    const abort = new AbortController()
    clearTimeout(timer.current)
    fetchProcesses(abort.signal)
      .then((result) => {
        if (abort.signal.aborted) return
        // Leitura com erro vem sem processos: mantém a última lista boa na tela.
        setData((previous) => (result.error && result.processes.length === 0 && previous ? previous : result))
        setError(result.error)
      })
      .catch((reason: unknown) => {
        if (!abort.signal.aborted) setError(reason instanceof Error ? reason.message : 'Falha ao ler os processos.')
      })
      .finally(() => {
        if (abort.signal.aborted) return
        setLoading(false)
        if (autoRef.current) timer.current = setTimeout(next, AUTO_REFRESH_MS)
      })
    return () => {
      abort.abort()
      clearTimeout(timer.current)
    }
  }, [tick, next])

  return { data, loading, error, auto, setAuto, refresh: next }
}
