import { dateTime } from '../modules/hardware/format'
import { NO_READING, intervalLabel, timeOfDay } from '../modules/live/format'
import { CpuPanel, DisksPanel, GpuPanel, MemoryPanel, SensorsPanel } from '../modules/live/panels'
import { CONNECTION_LABEL, MAX_POINTS, connectionState } from '../modules/live/series'
import { useLiveMonitor } from '../modules/live/useLiveMonitor'

/** Módulo C — Live Monitor. Apenas lê e exibe; não altera o sistema. */
export function LiveMonitorPage() {
  const { samples, status, everConnected, clientFailures, error, now } = useLiveMonitor()
  const latest = samples.length > 0 ? samples[samples.length - 1] : null
  const state = connectionState({ everConnected, clientFailures, status, latest, now })
  const intervalMs = status?.intervalMs ?? 2000
  // Acima disso entre dois pontos, a linha é interrompida (por exemplo, após o backend ficar fora do ar).
  const maxGapMs = Math.max(intervalMs * 3, 5000)
  const waiting = state === 'connecting' || state === 'waiting'

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <h1>PCCheck — Live Monitor</h1>
          <p className="muted">
            Última leitura: <strong>{latest ? timeOfDay(latest.collectedAt) : NO_READING}</strong>
            {status && (
              <>
                <br />
                Intervalo de coleta: {intervalLabel(status.intervalMs)} · Amostras: {samples.length}/{Math.min(status.historyCapacity, MAX_POINTS)}
                {latest && <> · Duração da última coleta: {latest.durationMs} ms</>}
              </>
            )}
          </p>
        </div>
        <p className={`connection connection-${state}`} role="status" aria-live="polite" data-state={state}>
          <span className="connection-dot" aria-hidden="true" />
          {CONNECTION_LABEL[state]}
        </p>
      </header>

      {state === 'disconnected' && (
        <div className="banner banner-error" role="alert">
          <strong>Sem conexão com o backend.</strong>
          <p>{error}</p>
          <p>
            {latest
              ? `Exibindo a última amostra recebida (${dateTime(latest.collectedAt)}). A conexão será retomada automaticamente.`
              : 'Nova tentativa automática em instantes.'}
          </p>
        </div>
      )}

      {state === 'degraded' && status && (
        <div className="banner banner-error" role="alert">
          <strong>A coleta está com problemas.</strong>
          <p>
            {status.stalled
              ? 'Uma coleta está demorando muito mais que o esperado.'
              : `${status.consecutiveFailures} falha(s) seguida(s) de coleta.`}
            {status.lastError ? ` Último erro: ${status.lastError}` : ''}
          </p>
          <p className="muted">Isso não indica defeito no hardware. Os dados exibidos são os da última coleta bem-sucedida.</p>
        </div>
      )}

      {state === 'stale' && (
        <div className="banner banner-error" role="alert">
          <strong>Sem amostras novas.</strong>
          <p>O backend responde, mas a última amostra é de {latest ? dateTime(latest.collectedAt) : NO_READING}.</p>
        </div>
      )}

      {waiting && (
        <p className="loading" role="status" aria-live="polite">
          {state === 'connecting' ? 'Conectando ao backend…' : 'Aguardando a primeira coleta terminar…'}
        </p>
      )}

      {latest && (
        <div className="grid grid-live">
          <CpuPanel samples={samples} maxGapMs={maxGapMs} />
          <MemoryPanel samples={samples} maxGapMs={maxGapMs} />
          <GpuPanel samples={samples} maxGapMs={maxGapMs} />
          <SensorsPanel samples={samples} maxGapMs={maxGapMs} />
          <DisksPanel samples={samples} maxGapMs={maxGapMs} />
        </div>
      )}

      <p className="muted">
        Os gráficos mostram somente leituras reais do computador local; métricas sem leitura aparecem como lacunas ou como
        &quot;Sem leitura&quot;. GPU e sensores vêm do LibreHardwareMonitor (servidor web local ativo). O histórico fica apenas na memória (últimas {MAX_POINTS} amostras).
      </p>
    </main>
  )
}
