import { bounds, project, solid, type IsoItem } from './iso'
import { IsoScene } from './IsoScene'

/** Camadas reais do PCCheck, de cima (o que o usuário vê) para baixo (de onde vêm os dados). */
const LAYERS: { name: string; detail: string }[] = [
  { name: 'Interface', detail: 'React · este navegador' },
  { name: 'API local', detail: 'Spring Boot · 127.0.0.1:8080' },
  { name: 'Coleta', detail: 'Agente OSHI · leitor de sensores' },
  { name: 'Sistema operacional', detail: 'WMI, SMART, /proc, /sys' },
]

const SIZE = 90
const GAP = 26

function scene() {
  const items: IsoItem[] = []
  const labels: { x: number; y: number; name: string; detail: string; top: boolean }[] = []
  LAYERS.forEach((layer, i) => {
    const z = (LAYERS.length - 1 - i) * GAP
    const plate = solid(0, 0, z, SIZE, SIZE, 5, i === 0 ? 'layer-top' : 'layer')
    if (i < LAYERS.length - 1) {
      // Ligação com a camada de baixo.
      plate.lines.push([[SIZE / 2, SIZE / 2, z], [SIZE / 2, SIZE / 2, z - GAP + 5]])
      plate.lineCls = 'iso-link'
    }
    items.push(plate)
    const [x, y] = project([SIZE, 0, z + 2.5])
    labels.push({ x: x + 16, y, name: layer.name, detail: layer.detail, top: i === 0 })
  })
  const extra = labels.flatMap((l): [number, number][] => [[l.x + 150, l.y - 10], [l.x + 150, l.y + 12]])
  return { items, labels, box: bounds(items, extra) }
}

const SCENE = scene()

export function ArchitectureStack({ className }: { className?: string }) {
  return (
    <IsoScene items={SCENE.items} box={SCENE.box} className={className}>
      {SCENE.labels.map((l) => (
        <g key={l.name}>
          <line x1={l.x - 12} y1={l.y} x2={l.x - 3} y2={l.y} className="iso-leader" />
          <text x={l.x} y={l.y - 2} className={l.top ? 'iso-label iso-label-top' : 'iso-label'}>{l.name}</text>
          <text x={l.x} y={l.y + 9} className="iso-label-detail">{l.detail}</text>
        </g>
      ))}
    </IsoScene>
  )
}
