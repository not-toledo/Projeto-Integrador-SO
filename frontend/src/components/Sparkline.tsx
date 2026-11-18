const W = 120
const H = 32

/**
 * Minigráfico de tendência (0 a `max`). Só desenha leituras reais: valores ausentes interrompem a linha.
 * Decorativo: o valor atual sempre aparece em texto ao lado.
 */
export function Sparkline({ values, max = 100 }: { values: (number | null)[]; max?: number }) {
  const n = values.length
  const step = n > 1 ? W / (n - 1) : 0
  const segments: string[] = []
  let current = ''
  values.forEach((v, i) => {
    if (typeof v !== 'number' || !Number.isFinite(v)) {
      if (current) segments.push(current)
      current = ''
      return
    }
    const y = H - (Math.min(Math.max(v, 0), max) / max) * (H - 2) - 1
    current += `${current ? 'L' : 'M'}${(i * step).toFixed(1)},${y.toFixed(1)}`
  })
  if (current) segments.push(current)

  return (
    <svg className="sparkline" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" aria-hidden="true" focusable="false">
      <line x1="0" y1={H - 0.5} x2={W} y2={H - 0.5} className="sparkline-base" />
      {segments.map((d, i) => <path key={i} d={d} className="sparkline-line" />)}
    </svg>
  )
}
