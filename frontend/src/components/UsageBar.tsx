import { NOT_AVAILABLE, percent } from '../modules/hardware/format'

/** Barra de utilização (0 a 100%). Sem valor válido, mostra "Não disponível" em vez de uma barra. */
export function UsageBar({ label, value }: { label: string; value: number | null }) {
  const valid = typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= 100
  return (
    <div className="usage">
      <div className="usage-label">
        <span>{label}</span>
        <strong>{valid ? percent(value) : NOT_AVAILABLE}</strong>
      </div>
      {valid && (
        <div
          className="usage-track"
          role="progressbar"
          aria-label={label}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(value)}
        >
          <div className="usage-fill" style={{ width: `${value}%` }} />
        </div>
      )}
    </div>
  )
}
