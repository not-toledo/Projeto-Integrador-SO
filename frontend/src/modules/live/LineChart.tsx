import { NO_READING, timeOfDay } from './format'
import { buildSegments, hasReadings, niceMax, segmentPath } from './series'

export interface ChartSeries {
  name: string
  values: (number | null)[]
  /** Traço tracejado: diferencia séries sem depender só da cor. */
  dashed?: boolean
}

interface LineChartProps {
  title: string
  /** Unidade do eixo vertical, sempre explícita (ex.: "%", "°C", "MiB/s"). */
  unit: string
  times: number[]
  series: ChartSeries[]
  /** Limite superior fixo (ex.: 100 para percentuais). Sem ele, a escala é automática. */
  yMax?: number
  /** Intervalo máximo entre pontos antes de interromper a linha (ms). */
  maxGapMs: number
  /** Casas decimais dos rótulos do eixo vertical. */
  digits?: number
  /** Rótulo dos instantes no eixo e na descrição (padrão: hora do dia). */
  formatTime?: (time: number) => string
}

const WIDTH = 640
const HEIGHT = 190
const PAD = { left: 52, right: 12, top: 12, bottom: 28 }

function label(value: number, digits: number): string {
  return value.toLocaleString('pt-BR', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

/**
 * Gráfico temporal em SVG, sem biblioteca externa. Mostra apenas leituras reais:
 * valores ausentes geram lacunas, e sem nenhuma leitura o gráfico informa "Sem leitura".
 */
export function LineChart({ title, unit, times, series, yMax, maxGapMs, digits = 0, formatTime = timeOfDay }: LineChartProps) {
  const anyReading = series.some((s) => hasReadings(s.values))
  const plotWidth = WIDTH - PAD.left - PAD.right
  const plotHeight = HEIGHT - PAD.top - PAD.bottom

  let top = yMax ?? 1
  if (yMax === undefined) {
    const max = Math.max(0, ...series.flatMap((s) => s.values.filter((v): v is number => typeof v === 'number' && Number.isFinite(v))))
    top = niceMax(max)
  }

  const first = times.length > 0 ? times[0] : 0
  const last = times.length > 0 ? times[times.length - 1] : 0
  const span = Math.max(last - first, 1)
  const toX = (time: number) => PAD.left + ((time - first) / span) * plotWidth
  const toY = (value: number) => PAD.top + plotHeight - (Math.min(value, top) / top) * plotHeight

  const ticks = [0, top / 2, top]
  const description = anyReading
    ? `Gráfico de ${title} em ${unit}, de ${formatTime(first)} a ${formatTime(last)}.`
    : `${title}: ${NO_READING}.`

  return (
    <figure className="chart">
      <figcaption>
        <strong>{title}</strong> <span className="muted">({unit})</span>
        {series.length > 1 && (
          <span className="chart-legend">
            {series.map((s, index) => (
              <span key={s.name} className="chart-legend-item">
                <svg width="22" height="8" aria-hidden="true">
                  <line className={`chart-line-${index}`} x1="0" y1="4" x2="22" y2="4" strokeWidth="2" strokeDasharray={s.dashed ? '4 3' : undefined} />
                </svg>
                {s.name}
              </span>
            ))}
          </span>
        )}
      </figcaption>
      <svg className="chart-svg" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={description}>
        {ticks.map((tick) => (
          <g key={tick}>
            <line className="chart-grid" x1={PAD.left} x2={WIDTH - PAD.right} y1={toY(tick)} y2={toY(tick)} />
            <text className="chart-axis" x={PAD.left - 6} y={toY(tick) + 4} textAnchor="end">
              {label(tick, digits)}
            </text>
          </g>
        ))}
        {anyReading && (
          <>
            <text className="chart-axis" x={PAD.left} y={HEIGHT - 8} textAnchor="start">
              {formatTime(first)}
            </text>
            <text className="chart-axis" x={WIDTH - PAD.right} y={HEIGHT - 8} textAnchor="end">
              {formatTime(last)}
            </text>
            {series.map((s, index) =>
              buildSegments(times, s.values, toX, toY, maxGapMs).map((segment, i) => (
                <path
                  key={`${s.name}-${i}`}
                  className={`chart-line chart-line-${index}`}
                  d={segmentPath(segment)}
                  strokeDasharray={s.dashed ? '6 4' : undefined}
                />
              )),
            )}
          </>
        )}
        {!anyReading && (
          <text className="chart-empty" x={WIDTH / 2} y={HEIGHT / 2} textAnchor="middle">
            {NO_READING}
          </text>
        )}
      </svg>
    </figure>
  )
}
