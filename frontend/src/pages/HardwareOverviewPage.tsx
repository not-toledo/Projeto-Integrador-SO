import { useInventory } from '../modules/hardware/useInventory'
import { CpuCard, GpuCard, MemoryCard, MotherboardCard, OsCard, StorageCard } from '../modules/hardware/cards'
import { dateTime } from '../modules/hardware/format'

/** Módulo A — Hardware Overview. */
export function HardwareOverviewPage() {
  const { inventory, loading, error, refresh } = useInventory()

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <h1>PCCheck — Hardware Overview</h1>
          {inventory && (
            <p className="muted">
              Última coleta: <strong>{dateTime(inventory.collectedAt)}</strong>
              <br />
              Origem: {inventory.source}
            </p>
          )}
        </div>
        <button type="button" onClick={refresh} disabled={loading}>
          {loading ? 'Atualizando…' : 'Atualizar'}
        </button>
      </header>

      {error && (
        <div className="banner banner-error" role="alert">
          <strong>Não foi possível obter o inventário.</strong>
          <p>{error}</p>
          {inventory && <p>Exibindo abaixo a última coleta bem-sucedida ({dateTime(inventory.collectedAt)}).</p>}
        </div>
      )}

      {!inventory && loading && (
        <p className="loading" role="status" aria-live="polite">
          Carregando inventário do computador…
        </p>
      )}

      {inventory && (
        <div className="grid">
          <OsCard result={inventory.os} />
          <CpuCard result={inventory.cpu} />
          <MemoryCard result={inventory.memory} />
          <GpuCard result={inventory.gpu} />
          <MotherboardCard result={inventory.motherboard} />
          <StorageCard result={inventory.storage} />
        </div>
      )}
    </main>
  )
}
