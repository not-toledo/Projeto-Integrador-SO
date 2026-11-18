import { PageHeader } from '../components/PageHeader'
import { useEffect, useState } from 'react'
import type {
  CompatibilityVerdict, ComponentCompatibility, GameCompatibility, GameSearchResult,
} from '../modules/games/types'
import { InstalledGames } from '../modules/games/InstalledGames'
import { PrecheckPanel } from '../modules/games/PrecheckPanel'
import { fetchGameCompatibility, searchGames } from '../services/gameService'

function statusLabel(status: ComponentCompatibility['status']) {
  return ({
    COMPATIBLE: 'Atendido',
    BELOW_MINIMUM: 'Abaixo do mínimo',
    UNKNOWN: 'Dados insuficientes',
    MANUAL_REVIEW: 'Revisão manual',
  })[status]
}
function statusClass(status: ComponentCompatibility['status']) {
  return status === 'COMPATIBLE' ? 'game-status-ok'
    : status === 'BELOW_MINIMUM' ? 'game-status-bad' : 'game-status-unknown'
}

export function GameCompatibilityPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<GameSearchResult[]>([])
  const [selected, setSelected] = useState<GameSearchResult | null>(null)
  const [comparison, setComparison] = useState<GameCompatibility | null>(null)
  const [searching, setSearching] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const term = query.trim()
    if (term.length < 2 || (selected && term === selected.name)) { setResults([]); setSearching(false); setError(''); return }
    let active = true
    const timer = window.setTimeout(async () => {
      setSearching(true)
      setError('')
      try {
        const data = await searchGames(term)
        if (active) setResults(data)
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : 'Falha na pesquisa.')
      } finally {
        if (active) setSearching(false)
      }
    }, 450)
    return () => { active = false; window.clearTimeout(timer) }
  }, [query, selected])

  async function choose(game: GameSearchResult, refresh = false) {
    setSelected(game)
    setComparison(null)
    setResults([])
    setLoading(true)
    setError('')
    try {
      setComparison(await fetchGameCompatibility(game.appId, refresh))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível comparar o jogo.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <PageHeader
        eyebrow="Módulo D · Game Compatibility"
        title="Compatibilidade com jogos"
      >
        <p className="muted">Pesquise jogos da Steam e compare os requisitos publicados com o hardware local.</p>
      </PageHeader>

      <section className="game-search">
        <label htmlFor="game-query">Nome do jogo</label>
        <input id="game-query" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Ex.: Counter-Strike 2" autoComplete="off" />
        {searching && <p className="muted" role="status">Pesquisando na Steam…</p>}
        {results.length > 0 && (
          <ul className="game-results">
            {results.map((game) => (
              <li key={game.appId}>
                <button type="button" className="game-result" onClick={() => { setQuery(game.name); void choose(game) }}>
                  {game.headerImage && <img src={game.headerImage} alt="" />}
                  <span><strong>{game.name}</strong><small>AppID: {game.appId}</small></span>
                  <span aria-hidden="true">Selecionar →</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {query.trim().length >= 2 && !searching && results.length === 0 && !error && (!selected || query.trim() !== selected.name) && (
          <p className="muted">Nenhum resultado encontrado.</p>
        )}
      </section>

      <InstalledGames onSelect={(game) => { setQuery(game.name); void choose(game) }} />

      {error && <div className="banner banner-error" role="alert">{error}</div>}
      {loading && <p className="loading" role="status">Obtendo requisitos e comparando com o inventário local…</p>}

      {selected && comparison && (
        <section className="game-comparison">
          <div className="game-title">
            {selected.headerImage && <img src={selected.headerImage} alt="" />}
            <div><h2>{comparison.gameName}</h2><a href={selected.storeUrl} target="_blank" rel="noreferrer">Abrir na Steam</a></div>
            <button type="button" onClick={() => void choose(selected, true)} disabled={loading}>Atualizar requisitos</button>
          </div>
          <VerdictBanner verdict={comparison.verdict} />
          <div className="grid">
            <article className="card">
              <h2>Requisitos mínimos</h2>
              <p className="game-overall">{comparison.overallMinimum}</p>
              <CompatibilityTable rows={comparison.minimumResults} />
            </article>
            <article className="card">
              <h2>Requisitos recomendados</h2>
              <p className="game-overall">{comparison.overallRecommended}</p>
              <CompatibilityTable rows={comparison.recommendedResults} />
            </article>
          </div>
          <PrecheckPanel appId={comparison.appId} />
          <details className="card game-raw">
            <summary>Requisitos publicados originalmente pela Steam</summary>
            <h3>Mínimos</h3><pre>{comparison.minimumRawText || 'Não publicados.'}</pre>
            <h3>Recomendados</h3><pre>{comparison.recommendedRawText || 'Não publicados.'}</pre>
            <p className="muted">Fonte: {comparison.source} · Consulta: {new Date(comparison.evaluatedAt).toLocaleString('pt-BR')}</p>
          </details>
          {comparison.warnings.length > 0 && <div className="banner game-warning"><strong>Limitações da análise</strong><ul>{comparison.warnings.map((w) => <li key={w}>{w}</li>)}</ul></div>}
          <p className="muted">Compatibilidade estimada com base nos requisitos publicados. Não garante FPS, estabilidade ou desempenho em todas as configurações.</p>
        </section>
      )}
    </main>
  )
}

const VERDICT_CLASS: Record<CompatibilityVerdict['code'], string> = {
  COMPATIBLE: 'game-verdict-ok',
  LIMITED: 'game-verdict-limited',
  NOT_RECOMMENDED: 'game-verdict-bad',
  INSUFFICIENT_DATA: 'game-verdict-unknown',
}

/** Classificação geral com os motivos; "Dados insuficientes" nunca é apresentado como compatível. */
function VerdictBanner({ verdict }: { verdict: CompatibilityVerdict }) {
  return (
    <section className={`game-verdict ${VERDICT_CLASS[verdict.code]}`} aria-label="Classificação geral">
      <p className="game-verdict-label">{verdict.label}</p>
      {verdict.reasons.length > 0 && <ul>{verdict.reasons.map((r) => <li key={r}>{r}</li>)}</ul>}
    </section>
  )
}

function CompatibilityTable({ rows }: { rows: ComponentCompatibility[] }) {
  if (rows.length === 0) return <p className="muted">A loja não publicou requisitos interpretáveis para esta seção.</p>
  return <div className="table-scroll"><table className="game-table">
    <thead><tr><th>Componente</th><th>Requisito Steam</th><th>Detectado</th><th>Resultado</th></tr></thead>
    <tbody>{rows.map((row, i) => <tr key={`${row.component}-${i}`}>
      <td>{row.component}</td><td>{row.requirement}</td><td>{row.detected}</td>
      <td><span className={`game-status ${statusClass(row.status)}`}>{statusLabel(row.status)}</span><small className="game-explanation">{row.explanation}</small>{row.benchmark && <small className="game-explanation">Comparado por benchmark ({row.benchmark.confidence === 'HIGH' ? 'confiança alta' : 'confiança média'}) · Fonte: {row.benchmark.source}</small>}</td>
    </tr>)}</tbody>
  </table></div>
}
