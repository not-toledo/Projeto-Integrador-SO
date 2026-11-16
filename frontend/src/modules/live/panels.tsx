import type { ReactNode } from 'react'
import { FieldList } from '../../components/FieldList'
import { UsageBar } from '../../components/UsageBar'
import type { ComponentResult } from '../hardware/types'
import { LineChart } from './LineChart'
import {
  NO_READING,
  celsius,
  isReading,
  liveBytes,
  livePercent,
  rate,
  rpm,
} from './format'
import {
  cpuTemperature,
  cpuTotal,
  driveBusy,
  driveNames,
  driveRateMiB,
  gpuTemperature,
  gpuUtilization,
  gpuVramPercent,
  hasReadings,
  memoryPercent,
  sampleTimes,
} from './series'
import type { LiveSample } from './types'

/** Rotação das ventoinhas: só leituras válidas; sem nenhuma, "Sem leitura". */
function fans(values: number[] | null): string {
  const valid = (values ?? []).filter(isReading)
  return valid.length === 0 ? NO_READING : valid.map(rpm).join(', ')
}

interface PanelProps {
  samples: LiveSample[]
  maxGapMs: number
}

/**
 * Cartão de um grupo de métricas. Distingue falha de coleta (ERROR) de ausência de dado (UNAVAILABLE),
 * e nenhum dos dois é apresentado como defeito do hardware.
 */
function LiveCard<T>({
  title,
  result,
  children,
}: {
  title: string
  result: ComponentResult<T> | undefined
  children: (data: T) => ReactNode
}) {
  let body: ReactNode
  if (result && result.status === 'OK' && result.data !== null) {
    body = children(result.data)
  } else if (result && result.status === 'ERROR') {
    body = (
      <div className="card-state card-state-error" role="alert">
        <strong>Falha na coleta</strong>
        <p>{result.message ?? 'A coleta desta métrica falhou.'}</p>
        <p className="muted">Isso não indica defeito no hardware; as demais métricas não foram afetadas.</p>
      </div>
    )
  } else {
    body = (
      <div className="card-state">
        <strong>{NO_READING}</strong>
        <p>{result?.message ?? 'O sistema não forneceu esta informação.'}</p>
      </div>
    )
  }
  return (
    <section className="card" aria-label={title}>
      <h2>{title}</h2>
      {body}
    </section>
  )
}

export function CpuPanel({ samples, maxGapMs }: PanelProps) {
  const latest = samples[samples.length - 1]
  const times = sampleTimes(samples)
  return (
    <LiveCard title="CPU" result={latest?.cpu}>
      {(data) => (
        <>
          <UsageBar label="Uso total" value={data.totalPercent} />
          <LineChart title="Uso da CPU" unit="%" times={times} series={[{ name: 'Uso total', values: samples.map(cpuTotal) }]} yMax={100} maxGapMs={maxGapMs} />
          <h3>Por processador lógico</h3>
          {data.perCorePercent === null ? (
            <p className="muted">{NO_READING}</p>
          ) : (
            <ul className="cores">
              {data.perCorePercent.map((value, index) => (
                <li key={index}>
                  <UsageBar label={`CPU ${index}`} value={value} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </LiveCard>
  )
}

export function MemoryPanel({ samples, maxGapMs }: PanelProps) {
  const latest = samples[samples.length - 1]
  const times = sampleTimes(samples)
  return (
    <LiveCard title="Memória RAM" result={latest?.memory}>
      {(data) => (
        <>
          <UsageBar label="Uso" value={data.usedPercent} />
          <FieldList
            fields={[
              ['Usada', liveBytes(data.usedBytes)],
              ['Disponível', liveBytes(data.availableBytes)],
              ['Total', liveBytes(data.totalBytes)],
            ]}
          />
          <LineChart title="Uso da RAM" unit="%" times={times} series={[{ name: 'Uso', values: samples.map(memoryPercent) }]} yMax={100} maxGapMs={maxGapMs} />
        </>
      )}
    </LiveCard>
  )
}

export function GpuPanel({ samples, maxGapMs }: PanelProps) {
  const latest = samples[samples.length - 1]
  const times = sampleTimes(samples)
  const load = samples.map(gpuUtilization)
  const vram = samples.map(gpuVramPercent)
  return (
    <LiveCard title="GPU" result={latest?.gpu}>
      {(data) => (
        <>
          <FieldList
            fields={[
              ['Placa', data.name ?? NO_READING],
              ['Utilização', livePercent(data.utilizationPercent)],
              ['Memória de vídeo em uso', liveBytes(data.vramUsedBytes)],
              ['Memória de vídeo total', liveBytes(data.vramTotalBytes)],
              ['Temperatura', celsius(data.temperatureCelsius)],
            ]}
          />
          {hasReadings(load) || hasReadings(vram) ? (
            <LineChart
              title="Uso da GPU e da memória de vídeo"
              unit="%"
              times={times}
              series={[
                { name: 'Utilização', values: load },
                { name: 'Memória de vídeo', values: vram, dashed: true },
              ]}
              yMax={100}
              maxGapMs={maxGapMs}
            />
          ) : (
            <p className="muted">Sem leitura de utilização neste computador; nenhum valor é estimado.</p>
          )}
        </>
      )}
    </LiveCard>
  )
}

export function DisksPanel({ samples, maxGapMs }: PanelProps) {
  const latest = samples[samples.length - 1]
  const times = sampleTimes(samples)
  const names = driveNames(samples)
  return (
    <LiveCard title="Discos" result={latest?.disks}>
      {(data) => (
        <>
          <h3>Atividade das unidades</h3>
          {names.length === 0 ? (
            <p className="muted">{NO_READING}: o sistema não informou a atividade das unidades.</p>
          ) : (
            names.map((name) => {
              const current = data.drives?.find((d) => d.name === name)
              const read = samples.map((s) => driveRateMiB(s, name, 'read'))
              const write = samples.map((s) => driveRateMiB(s, name, 'write'))
              return (
                <div key={name} className="drive">
                  <p className="drive-now">
                    <strong>{name}</strong> — leitura {rate(current?.readBytesPerSecond)}, escrita {rate(current?.writeBytesPerSecond)}, ocupado{' '}
                    {livePercent(current?.busyPercent)}
                  </p>
                  <LineChart
                    title={`Leitura e escrita — ${name}`}
                    unit="MiB/s"
                    times={times}
                    series={[
                      { name: 'Leitura', values: read },
                      { name: 'Escrita', values: write, dashed: true },
                    ]}
                    maxGapMs={maxGapMs}
                    digits={1}
                  />
                  <LineChart
                    title={`Tempo ocupado — ${name}`}
                    unit="%"
                    times={times}
                    series={[{ name: 'Ocupado', values: samples.map((s) => driveBusy(s, name)) }]}
                    yMax={100}
                    maxGapMs={maxGapMs}
                  />
                </div>
              )
            })
          )}
          <h3>Espaço livre dos volumes</h3>
          {data.volumes === null || data.volumes.length === 0 ? (
            <p className="muted">{NO_READING}</p>
          ) : (
            <ul className="volumes">
              {data.volumes.map((volume, index) => (
                <li key={`${volume.mountPoint ?? 'volume'}-${index}`}>
                  <UsageBar label={`${volume.mountPoint ?? volume.label ?? 'Volume'} — ocupação`} value={volume.usedPercent} />
                  <p className="muted">
                    Livre: {liveBytes(volume.freeBytes)} de {liveBytes(volume.totalBytes)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </LiveCard>
  )
}

export function SensorsPanel({ samples, maxGapMs }: PanelProps) {
  const latest = samples[samples.length - 1]
  const times = sampleTimes(samples)
  const temps = samples.map(cpuTemperature)
  const gpuTemps = samples.map(gpuTemperature)
  return (
    <LiveCard title="Sensores" result={latest?.sensors}>
      {(data) => (
        <>
          <FieldList
            fields={[
              ['Temperatura da CPU', celsius(data.cpuTemperatureCelsius)],
              ['Temperatura da GPU', celsius(data.gpuTemperatureCelsius)],
              ['Ventoinhas', fans(data.fanSpeedsRpm)],
            ]}
          />
          {hasReadings(temps) || hasReadings(gpuTemps) ? (
            <LineChart
              title="Temperaturas"
              unit="°C"
              times={times}
              series={[
                { name: 'CPU', values: temps },
                { name: 'GPU', values: gpuTemps, dashed: true },
              ]}
              maxGapMs={maxGapMs}
              digits={0}
            />
          ) : (
            <p className="muted">Sem leitura de temperatura neste computador; nenhum valor é estimado.</p>
          )}
        </>
      )}
    </LiveCard>
  )
}
