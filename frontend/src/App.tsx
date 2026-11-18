import { useState } from 'react'
import { HardwareOverviewPage } from './pages/HardwareOverviewPage'
import { HealthCheckPage } from './pages/HealthCheckPage'
import { LiveMonitorPage } from './pages/LiveMonitorPage'
import { GameCompatibilityPage } from './pages/GameCompatibilityPage'
import { HistoryPage } from './pages/HistoryPage'

type View = 'hardware' | 'health' | 'live' | 'history' | 'games'

const VIEWS: { id: View; label: string }[] = [
  { id: 'hardware', label: 'Hardware Overview' },
  { id: 'health', label: 'Health Check' },
  { id: 'live', label: 'Live Monitor' },
  { id: 'history', label: 'Histórico' },
  { id: 'games', label: 'Compatibilidade com jogos' },
]

function App() {
  const [view, setView] = useState<View>('hardware')

  return (
    <>
      <nav className="nav" aria-label="Módulos">
        {VIEWS.map(({ id, label }) => (
          <button
            key={id}
            type="button"
            className={view === id ? 'nav-item nav-item-active' : 'nav-item'}
            aria-current={view === id ? 'page' : undefined}
            onClick={() => setView(id)}
          >
            {label}
          </button>
        ))}
      </nav>
      {view === 'hardware' && <HardwareOverviewPage />}
      {view === 'health' && <HealthCheckPage />}
      {view === 'live' && <LiveMonitorPage />}
      {view === 'history' && <HistoryPage />}
      {view === 'games' && <GameCompatibilityPage />}
    </>
  )
}

export default App
