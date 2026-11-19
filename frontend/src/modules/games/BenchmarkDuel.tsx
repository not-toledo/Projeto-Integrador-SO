import { duelHeadline, duelResult } from './duel'
import type { BenchmarkComparison } from './types'

const score = (v: number) => `${v.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} pts`

/**
 * Comparação lado a lado do benchmark (Blender Open Data): duas barras na mesma escala, com o vencedor marcado.
 * A pontuação mede renderização, não FPS; o texto completo fica nos detalhes.
 */
export function BenchmarkDuel({ benchmark, component }: { benchmark: BenchmarkComparison; component: string }) {
  const result = duelResult(benchmark.ratio)
  const max = Math.max(benchmark.localScore, benchmark.requiredScore, 1)
  const rows = [
    { who: 'Seu', name: benchmark.localModel, value: benchmark.localScore, wins: result === 'local' },
    { who: 'Exigido', name: benchmark.requiredModel, value: benchmark.requiredScore, wins: result === 'required' },
  ]
  return (
    <div className={`duel duel-${result}`} aria-label={`Comparação de desempenho: ${duelHeadline(benchmark.ratio, component)}`}>
      <p className="duel-headline">{duelHeadline(benchmark.ratio, component)}</p>
      {rows.map((r) => (
        <div key={r.who} className={r.wins ? 'duel-row duel-winner' : 'duel-row'}>
          <span className="duel-who">{r.who}</span>
          <span className="duel-name" title={r.name}>{r.name}</span>
          <span className="duel-score">{score(r.value)}</span>
          <span className="duel-track" aria-hidden="true">
            <span className="duel-fill" style={{ width: `${(r.value / max) * 100}%` }} />
          </span>
        </div>
      ))}
    </div>
  )
}
