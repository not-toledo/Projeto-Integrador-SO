/**
 * Projeção isométrica mínima para as ilustrações (SVG gerado a partir de caixas 3D, sem biblioteca).
 * Faces visíveis: topo, frente-esquerda e frente-direita; a ordem de desenho é por profundidade.
 */
export type P = readonly [number, number, number]
type Face = { pts: P[]; cls: string }
export type IsoItem = { depth: number; faces: Face[]; lines: [P, P][]; lineCls: string; float?: number }

const COS30 = Math.cos(Math.PI / 6)

export function project([x, y, z]: P): [number, number] {
  return [(x - y) * COS30, (x + y) * 0.5 - z]
}

export function solid(x: number, y: number, z: number, w: number, d: number, h: number, kind: string, float?: number): IsoItem {
  return {
    depth: x + w / 2 + y + d / 2 + z,
    faces: [
      { pts: [[x, y + d, z], [x + w, y + d, z], [x + w, y + d, z + h], [x, y + d, z + h]], cls: `iso-${kind} iso-left` },
      { pts: [[x + w, y, z], [x + w, y + d, z], [x + w, y + d, z + h], [x + w, y, z + h]], cls: `iso-${kind} iso-right` },
      { pts: [[x, y, z + h], [x + w, y, z + h], [x + w, y + d, z + h], [x, y + d, z + h]], cls: `iso-${kind} iso-top` },
    ],
    lines: [],
    lineCls: '',
    float,
  }
}

/** Cubo de arestas (as 12, como vidro), com faces quase transparentes. */
export function wire(x: number, y: number, z: number, s: number, float?: number): IsoItem {
  const c = (i: number, j: number, k: number): P => [x + i * s, y + j * s, z + k * s]
  const edges: [P, P][] = []
  for (const [a, b] of [[0, 0], [1, 0], [0, 1], [1, 1]] as const) {
    edges.push([c(a, b, 0), c(a, b, 1)], [c(0, a, b), c(1, a, b)], [c(a, 0, b), c(a, 1, b)])
  }
  return { ...solid(x, y, z, s, s, s, 'glass', float), lines: edges, lineCls: 'iso-wire' }
}

/** Caixa de enquadramento (x, y, largura, altura) das peças projetadas, com margem. */
export function bounds(items: IsoItem[], extra: [number, number][] = [], pad = 8): [number, number, number, number] {
  const all = [...items.flatMap((it) => [...it.faces.flatMap((f) => f.pts), ...it.lines.flat()]).map(project), ...extra]
  const xs = all.map(([x]) => x)
  const ys = all.map(([, y]) => y)
  const minX = Math.min(...xs) - pad
  const minY = Math.min(...ys) - pad
  return [minX, minY, Math.max(...xs) - minX + pad, Math.max(...ys) - minY + pad]
}
