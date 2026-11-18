import { bounds, solid, wire, type IsoItem } from './iso'
import { IsoScene } from './IsoScene'

/**
 * Ilustração do hero: um processador em projeção isométrica, em linhas, com cubos soltos ao redor (dados).
 * Puramente decorativa.
 */
function scene(): IsoItem[] {
  const items: IsoItem[] = []
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
  return items
}

const ITEMS = scene()
const BOX = bounds(ITEMS)

export function IsoChip({ className }: { className?: string }) {
  return <IsoScene items={ITEMS} box={BOX} className={className} />
}
