import { useEffect, useState } from 'react'
import { fetchLive, fetchLiveHistory } from '../../services/liveService'
import { MAX_POINTS, appendSample, limitSamples } from './series'
import type { LiveSample, MonitorStatus } from './types'

export interface LiveMonitorState {
  /** Histórico limitado (no máximo MAX_POINTS), da amostra mais antiga para a mais recente. */
  samples: LiveSample[]
  status: MonitorStatus | null
  /** Houve ao menos uma resposta do backend. */
  everConnected: boolean
  /** Requisições seguidas que falharam. */
  clientFailures: number
  /** Mensagem do último erro de comunicação, ou null. */
  error: string | null
  /** Horário (ms) em que a tela foi atualizada pela última vez; serve de referência para detectar dados parados. */
  now: number
}

const DEFAULT_INTERVAL_MS = 2000
const MIN_POLL_MS = 1000
const MAX_POLL_MS = 10_000

/** Intervalo de consulta: acompanha o intervalo de coleta do backend, entre 1 s e 10 s. */
export function pollInterval(status: MonitorStatus | null): number {
  const base = status?.intervalMs ?? DEFAULT_INTERVAL_MS
  return Math.min(Math.max(base, MIN_POLL_MS), MAX_POLL_MS)
}

/** Espera maior após falhas seguidas (2 s, 4 s, 8 s… até 10 s), para não insistir em um backend fora do ar. */
export function backoffInterval(failures: number): number {
  return Math.min(DEFAULT_INTERVAL_MS * 2 ** Math.max(failures - 1, 0), MAX_POLL_MS)
}

/**
 * Consulta o backend periodicamente. A próxima consulta só é agendada DEPOIS que a anterior termina
 * (setTimeout encadeado), então nunca há requisições acumuladas. A coleta de hardware em si ocorre
 * no backend, em segundo plano; aqui apenas se lê a memória dele, sem bloquear a interface.
 * A primeira consulta carrega o histórico; as seguintes trazem só a amostra mais recente.
 */
export function useLiveMonitor(): LiveMonitorState {
  const [state, setState] = useState<LiveMonitorState>({
    samples: [],
    status: null,
    everConnected: false,
    clientFailures: 0,
    error: null,
    now: Date.now(),
  })

  useEffect(() => {
    const abort = new AbortController()
    let timer: ReturnType<typeof setTimeout> | undefined
    let samples: LiveSample[] = []
    let status: MonitorStatus | null = null
    let initialized = false
    let failures = 0

    const tick = async () => {
      try {
        if (!initialized) {
          const history = await fetchLiveHistory(MAX_POINTS, abort.signal)
          samples = limitSamples(history.samples)
          status = history.status
          initialized = true
        } else {
          const live = await fetchLive(abort.signal)
          status = live.status
          if (live.latest) {
            samples = appendSample(samples, live.latest)
          }
        }
        failures = 0
        setState({ samples, status, everConnected: true, clientFailures: 0, error: null, now: Date.now() })
      } catch (reason: unknown) {
        if (abort.signal.aborted) {
          return
        }
        failures += 1
        const error = reason instanceof Error ? reason.message : 'Falha desconhecida ao consultar o Live Monitor.'
        setState((previous) => ({ ...previous, clientFailures: failures, error, now: Date.now() }))
      }
      if (!abort.signal.aborted) {
        timer = setTimeout(tick, failures > 0 ? backoffInterval(failures) : pollInterval(status))
      }
    }

    void tick()

    return () => {
      abort.abort()
      if (timer !== undefined) {
        clearTimeout(timer)
      }
    }
  }, [])

  return state
}
