import type { ReactNode } from 'react'

/**
 * Destaque no topo de um card (mesmo padrão dos alertas da Saúde): valor grande, legenda curta e, quando há um
 * percentual medido, uma régua. Sem valor medido não há régua (nada é estimado).
 */
export function Spot({ value, caption, missing = false, gauge }: {
  value: string
  caption?: ReactNode
  /** O valor é uma ausência ("Sem leitura", "Não disponível"): aparece discreto. */
  missing?: boolean
  gauge?: { label: string; percent: number | null; text: string }
}) {
  const valid = gauge && typeof gauge.percent === 'number' && Number.isFinite(gauge.percent)
  return (
    <div className="spot">
      <p className={missing ? 'spot-value spot-missing' : 'spot-value'}>{value}</p>
      {caption && <p className="spot-caption">{caption}</p>}
      {gauge && valid && (
        <div className="gauge">
          <div className="gauge-track" role="progressbar" aria-label={gauge.label} aria-valuemin={0} aria-valuemax={100}
            aria-valuenow={Math.round(gauge.percent as number)}>
            <div className="gauge-fill gauge-fill-info" style={{ width: `${Math.min(Math.max(gauge.percent as number, 0), 100)}%` }} />
          </div>
          <div className="gauge-legend">
            <span>{gauge.label}</span>
            <span>{gauge.text}</span>
          </div>
        </div>
      )}
    </div>
  )
}
