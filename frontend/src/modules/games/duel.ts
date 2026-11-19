/** Diferenças menores que 3% contam como empate (mesma regra da frase do backend). */
const TIE = 0.03

export type DuelResult = 'local' | 'tie' | 'required'

export function duelResult(ratio: number): DuelResult {
  if (Math.abs(ratio - 1) < TIE) return 'tie'
  return ratio > 1 ? 'local' : 'required'
}

/** Manchete curta de quem é melhor, sempre com texto (não depende só da cor). */
export function duelHeadline(ratio: number, component: string): string {
  const yours = component === 'GPU' ? 'A sua placa' : 'O seu processador'
  const pct = (v: number) => Math.round(v * 100)
  switch (duelResult(ratio)) {
    case 'tie':
      return 'Praticamente iguais'
    case 'local':
      return `${yours} é melhor: ${pct(ratio - 1)}% mais rápido`
    default:
      return `O exigido é melhor: ${yours.toLowerCase()} rende ${pct(ratio)}%`
  }
}
