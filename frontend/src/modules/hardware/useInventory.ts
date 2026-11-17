import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchInventory } from '../../services/inventoryService'
import type { Inventory } from './types'

export interface InventoryState {
  /** Última coleta recebida com sucesso (mantida se uma atualização falhar). */
  inventory: Inventory | null
  /** true enquanto uma requisição estiver em andamento. */
  loading: boolean
  /** Mensagem do último erro de comunicação, ou null. */
  error: string | null
  refresh: () => void
}

/** Carrega o inventário ao montar e a cada atualização manual; nunca dispara duas requisições ao mesmo tempo. */
export function useInventory(): InventoryState {
  const [inventory, setInventory] = useState<Inventory | null>(null)
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

    fetchInventory(abort.signal)
      .then((result) => {
        if (!abort.signal.aborted) {
          setInventory(result)
        }
      })
      .catch((reason: unknown) => {
        if (!abort.signal.aborted) {
          setError(reason instanceof Error ? reason.message : 'Falha desconhecida ao obter o inventário.')
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

  return { inventory, loading, error, refresh: load }
}
