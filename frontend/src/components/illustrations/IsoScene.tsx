import type { ReactNode } from 'react'
import { project, type IsoItem, type P } from './iso'

const points = (ps: P[]) => ps.map((p) => project(p).map((v) => v.toFixed(1)).join(',')).join(' ')

/** Desenha as peças isométricas em ordem de profundidade. Decorativo (aria-hidden). */
export function IsoScene({ items, box, className, children }: {
  items: IsoItem[]
  box: [number, number, number, number]
  className?: string
  /** Elementos extras (ex.: rótulos) no mesmo sistema de coordenadas. */
  children?: ReactNode
}) {
  const sorted = [...items].sort((a, b) => a.depth - b.depth)
  return (
    <svg className={className} viewBox={box.map((v) => v.toFixed(1)).join(' ')} aria-hidden="true" focusable="false">
      {sorted.map((it, i) => (
        <g key={i} className={it.float === undefined ? undefined : `iso-float iso-float-${it.float}`}>
          {it.faces.map((f, j) => <polygon key={j} points={points(f.pts)} className={f.cls} />)}
          {it.lines.map(([a, b], j) => {
            const [x1, y1] = project(a)
            const [x2, y2] = project(b)
            return <line key={`l${j}`} x1={x1} y1={y1} x2={x2} y2={y2} className={it.lineCls} />
          })}
        </g>
      ))}
      {children}
    </svg>
  )
}
