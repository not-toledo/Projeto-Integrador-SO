import { useEffect } from 'react'
import { SiteFooter } from './components/SiteFooter'
import { SiteHeader } from './components/SiteHeader'
import { useView } from './navigation'
import { GameCompatibilityPage } from './pages/GameCompatibilityPage'
import { HardwareOverviewPage } from './pages/HardwareOverviewPage'
import { HealthCheckPage } from './pages/HealthCheckPage'
import { HistoryPage } from './pages/HistoryPage'
import { HomePage } from './pages/HomePage'
import { LiveMonitorPage } from './pages/LiveMonitorPage'

function App() {
  const view = useView()

  // Cada tela começa do topo.
  useEffect(() => {
    window.scrollTo?.(0, 0)
  }, [view])

  return (
    <div className="site">
      {/* O fragmento do endereço guarda a tela atual: o atalho move o foco sem trocar o endereço. */}
      <a className="skip-link" href="#conteudo" onClick={(e) => {
        e.preventDefault()
        document.getElementById('conteudo')?.focus()
      }}>Pular para o conteúdo</a>
      <SiteHeader current={view} />
      <div id="conteudo" className="site-main" tabIndex={-1}>
        {view === 'home' && <HomePage />}
        {view === 'hardware' && <HardwareOverviewPage />}
        {view === 'health' && <HealthCheckPage />}
        {view === 'live' && <LiveMonitorPage />}
        {view === 'history' && <HistoryPage />}
        {view === 'games' && <GameCompatibilityPage />}
      </div>
      <SiteFooter />
    </div>
  )
}

export default App
