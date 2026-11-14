import { useState } from 'react'
import { HardwareOverviewPage } from './pages/HardwareOverviewPage'
import { HealthCheckPage } from './pages/HealthCheckPage'
import { LiveMonitorPage } from './pages/LiveMonitorPage'

type View = 'hardware' | 'health' | 'live'

const VIEWS: { id: View; label: string }[] = [
  { id: 'hardware', label: 'Hardware Overview' },
  { id: 'health', label: 'Health Check' },
  { id: 'live', label: 'Live Monitor' },
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
    </>
  )
}

export default App
