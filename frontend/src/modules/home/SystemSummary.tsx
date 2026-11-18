import type { ReactNode } from 'react'
import { ArrowIcon } from '../../components/Icons'
import { hrefOf } from '../../navigation'
import { NOT_AVAILABLE, bytes, count, text } from '../hardware/format'
import type { ComponentResult } from '../hardware/types'
import { useInventory } from '../hardware/useInventory'

const COLLECTION_FAILED = 'Falha na coleta'

/** Valor de um componente do inventário, distinguindo "não disponível" de falha de coleta. */
function value<T>(result: ComponentResult<T>, read: (data: T) => string): string {
  if (result.status === 'OK' && result.data !== null) return read(result.data)
  return result.status === 'ERROR' ? COLLECTION_FAILED : NOT_AVAILABLE
}

function Stat({ value: v, label }: { value: string; label: string }) {
  const missing = v === NOT_AVAILABLE || v === COLLECTION_FAILED
  return (
    <div className="stat">
      <dt className="stat-label">{label}</dt>
      <dd className={missing ? 'stat-value stat-missing' : 'stat-value'}>{v}</dd>
    </div>
  )
}

/** "Seu computador": resumo real do inventário (GET /api/inventory), com atalho para o Hardware. */
export function SystemSummary() {
  const { inventory, loading, error, refresh } = useInventory()

  let body: ReactNode
  if (!inventory) {
    body = loading ? (
      <p className="loading" role="status" aria-live="polite">Lendo o inventário deste computador…</p>
    ) : (
      <div className="banner banner-error" role="alert">
        <strong>Não foi possível obter o inventário.</strong>
        <p>{error}</p>
        <p>Confira se o backend está aberto (scripts\run-backend.bat) e tente de novo.</p>
        <button type="button" onClick={refresh}>Tentar de novo</button>
      </div>
    )
  } else {
    const { os, cpu, memory, gpu, storage } = inventory
    const specs: [string, string][] = [
      ['Sistema', value(os, (o) => [o.name, o.version, o.buildNumber && `(build ${o.buildNumber})`].filter(Boolean).join(' ') || NOT_AVAILABLE)],
      ['Processador', value(cpu, (c) => text(c.name))],
      ['Placa de vídeo', value(gpu, (g) => (g.length > 0 ? g.map((x) => text(x.name)).join(' + ') : NOT_AVAILABLE))],
      ['Memória', value(memory, (m) => bytes(m.totalBytes))],
      ['Armazenamento', value(storage, (s) => (s.drives && s.drives.length > 0
        ? s.drives.map((d) => [bytes(d.sizeBytes), d.mediaType ?? d.busType].filter(Boolean).join(' ')).join(' · ')
        : NOT_AVAILABLE))],
    ]
    body = (
      <>
        <div className="system">
          <p className="vertical-label" aria-hidden="true">Seu computador</p>
          <div className="system-main">
            <h3 className="system-name">{value(cpu, (c) => text(c.name))}</h3>
            <dl className="spec-list">
              {specs.map(([label, v]) => (
                <div key={label} className="spec">
                  <dt>{label}:</dt>
                  <dd>{v}</dd>
                </div>
              ))}
            </dl>
            <div className="actions">
              <a className="btn btn-primary" href={hrefOf('hardware')}>Ver inventário completo <ArrowIcon /></a>
              <a className="link-arrow" href={hrefOf('health')}>Verificar a saúde <ArrowIcon /></a>
            </div>
          </div>
        </div>
        <dl className="stats" aria-label="Números deste computador">
          <Stat value={value(cpu, (c) => count(c.physicalCores))} label="núcleos físicos" />
          <Stat value={value(cpu, (c) => count(c.logicalProcessors))} label="processadores lógicos" />
          <Stat value={value(memory, (m) => bytes(m.totalBytes))} label="de memória RAM" />
          <Stat value={value(storage, (s) => (s.drives ? String(s.drives.length) : NOT_AVAILABLE))} label="discos físicos" />
        </dl>
      </>
    )
  }

  return (
    <section className="section" aria-labelledby="system-title">
      {/* Com o inventário, o rótulo vertical faz o papel visual do título. */}
      <h2 id="system-title" className={inventory ? 'sr-only' : 'section-title display'}>Seu computador</h2>
      {body}
    </section>
  )
}
