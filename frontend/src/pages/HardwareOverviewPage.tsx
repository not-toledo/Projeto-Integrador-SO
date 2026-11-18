import { PageHeader } from '../components/PageHeader'
import { useInventory } from '../modules/hardware/useInventory'
import { CpuCard, GpuCard, MemoryCard, MotherboardCard, OsCard, StorageCard } from '../modules/hardware/cards'
import { dateTime } from '../modules/hardware/format'
import { platformOf } from '../modules/sources/sourceCatalog'

/** Módulo A — Hardware Overview. */
export function HardwareOverviewPage() {
  const { inventory, loading, error, refresh } = useInventory()
  const platform = platformOf(inventory?.os.status === 'OK' ? inventory.os.data?.name : null)

  return (
    <main className="page">
      <PageHeader
        eyebrow="Módulo A · Hardware Overview"
        title="Hardware"
        actions={
          <button type="button" onClick={refresh} disabled={loading}>
            {loading ? 'Atualizando…' : 'Atualizar'}
          </button>
        }
      >
        {inventory && (
          <p className="muted">
            Última coleta: <strong>{dateTime(inventory.collectedAt)}</strong>
            <br />
            Origem: {inventory.source}
          </p>
        )}
      </PageHeader>

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
          <OsCard result={inventory.os} platform={platform} />
          <CpuCard result={inventory.cpu} platform={platform} />
          <MemoryCard result={inventory.memory} platform={platform} />
          <GpuCard result={inventory.gpu} platform={platform} />
          <MotherboardCard result={inventory.motherboard} platform={platform} />
          <StorageCard result={inventory.storage} platform={platform} />
        </div>
      )}
    </main>
  )
}
