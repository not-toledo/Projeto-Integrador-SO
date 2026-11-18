/**
 * Ilustração do hero: um processador em projeção isométrica, em linhas, com cubos soltos ao redor (dados).
 * Gerada em SVG a partir de caixas 3D simples, sem imagem e sem biblioteca. Puramente decorativa.
 */
type P = readonly [number, number, number]
type Face = { pts: P[]; cls: string }
type Item = { depth: number; faces: Face[]; lines: [P, P][]; lineCls: string; float?: number }

const COS30 = Math.cos(Math.PI / 6)

function project([x, y, z]: P): [number, number] {
  return [(x - y) * COS30, (x + y) * 0.5 - z]
}

function solid(x: number, y: number, z: number, w: number, d: number, h: number, kind: string, float?: number): Item {
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

/** Cubo de arestas (todas as 12, como vidro), com faces quase transparentes. */
function wire(x: number, y: number, z: number, s: number, float?: number): Item {
  const c = (i: number, j: number, k: number): P => [x + i * s, y + j * s, z + k * s]
  const edges: [P, P][] = []
  for (const [a, b] of [[0, 0], [1, 0], [0, 1], [1, 1]] as const) {
    edges.push([c(a, b, 0), c(a, b, 1)], [c(0, a, b), c(1, a, b)], [c(a, 0, b), c(a, 1, b)])
  }
  const item = solid(x, y, z, s, s, s, 'glass', float)
  return { ...item, lines: edges, lineCls: 'iso-wire' }
}

function scene(): Item[] {
  const items: Item[] = []
  const pkg = solid(0, 0, 0, 100, 100, 5, 'pkg')
  // Trilhas na placa, do núcleo até as bordas.
  for (const t of [36, 50, 64]) {
    pkg.lines.push([[72, t, 5], [96, t, 5]], [[t, 72, 5], [t, 96, 5]])
  }
  pkg.lineCls = 'iso-trace'
  items.push(pkg)

  for (let i = 0; i < 9; i++) {
    const o = 8 + i * 10.5
    items.push(solid(o, 100, 0, 4, 7, 1.5, 'pin'), solid(100, o, 0, 7, 4, 1.5, 'pin'))
    items.push(solid(o, -7, 0, 4, 7, 1.5, 'pin'), solid(-7, o, 0, 7, 4, 1.5, 'pin'))
  }

  const die = solid(28, 28, 5, 44, 44, 7, 'die')
  for (const t of [11, 22, 33]) {
    die.lines.push([[28 + t, 28, 12], [28 + t, 72, 12]], [[28, 28 + t, 12], [72, 28 + t, 12]])
  }
  die.lineCls = 'iso-grid'
  items.push(die)

  items.push(wire(-70, 20, 30, 26, 0), wire(20, -78, 52, 22, 1), wire(-36, -46, 92, 18, 2), wire(120, 36, 42, 20, 1))

  const bits: [number, number, number, number][] = [
    [-30, 60, 40, 6], [-60, -10, 70, 5], [0, -40, 90, 4], [60, -50, 70, 6], [100, -10, 60, 5],
    [130, 90, 20, 4], [-50, 70, 10, 4], [40, 120, 22, 5], [80, -90, 40, 4], [-90, 40, 60, 4],
    [10, -10, 120, 5], [110, 110, 50, 4], [-20, 130, 30, 4], [150, 10, 80, 4],
  ]
  bits.forEach(([x, y, z, s], i) => items.push(solid(x, y, z, s, s, s, 'bit', i % 3)))

  return items.sort((a, b) => a.depth - b.depth)
}

const ITEMS = scene()

function viewBox(items: Item[]): string {
  const xs: number[] = []
  const ys: number[] = []
  for (const it of items) {
    for (const p of [...it.faces.flatMap((f) => f.pts), ...it.lines.flat()]) {
      const [x, y] = project(p)
      xs.push(x)
      ys.push(y)
    }
  }
  const pad = 8
  const minX = Math.min(...xs) - pad
  const minY = Math.min(...ys) - pad
  return `${minX.toFixed(1)} ${minY.toFixed(1)} ${(Math.max(...xs) - minX + pad).toFixed(1)} ${(Math.max(...ys) - minY + pad).toFixed(1)}`
}

const VIEW_BOX = viewBox(ITEMS)
const pts = (ps: P[]) => ps.map((p) => project(p).map((v) => v.toFixed(1)).join(',')).join(' ')

export function IsoChip({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox={VIEW_BOX} aria-hidden="true" focusable="false">
      {ITEMS.map((it, i) => (
        <g key={i} className={it.float === undefined ? undefined : `iso-float iso-float-${it.float}`}>
          {it.faces.map((f, j) => <polygon key={j} points={pts(f.pts)} className={f.cls} />)}
          {it.lines.map(([a, b], j) => {
            const [x1, y1] = project(a)
            const [x2, y2] = project(b)
            return <line key={`l${j}`} x1={x1} y1={y1} x2={x2} y2={y2} className={it.lineCls} />
          })}
        </g>
      ))}
    </svg>
  )
}
