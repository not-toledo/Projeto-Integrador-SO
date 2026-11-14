import type { ReactNode } from 'react'
import { NOT_AVAILABLE } from '../modules/hardware/format'
import type { ComponentResult } from '../modules/hardware/types'

interface ComponentCardProps<T> {
  title: string
  result: ComponentResult<T>
  /** Renderiza o conteúdo quando há dados. */
  children: (data: T) => ReactNode
}

/**
 * Cartão de um componente. Trata os três estados do coletor:
 * OK (dados), UNAVAILABLE (o sistema não informou) e ERROR (a coleta falhou).
 * Indisponibilidade e falha de coleta não são apresentadas como defeito do hardware.
 */
export function ComponentCard<T>({ title, result, children }: ComponentCardProps<T>) {
  let body: ReactNode
  if (result.status === 'OK' && result.data !== null) {
    body = children(result.data)
  } else if (result.status === 'ERROR') {
    body = (
      <div className="card-state card-state-error" role="alert">
        <strong>Falha na coleta</strong>
        <p>{result.message ?? 'A coleta deste componente falhou.'}</p>
        <p className="muted">Isso não indica defeito no hardware; os demais componentes não foram afetados.</p>
      </div>
    )
  } else {
    body = (
      <div className="card-state">
        <strong>{NOT_AVAILABLE}</strong>
        <p>{result.message ?? 'O sistema não forneceu esta informação.'}</p>
      </div>
    )
  }

  return (
    <section className="card" aria-label={title}>
      <h2>{title}</h2>
      {body}
    </section>
  )
}
