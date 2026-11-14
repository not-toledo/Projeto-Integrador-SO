import { useState } from 'react'
import { HardwareOverviewPage } from './pages/HardwareOverviewPage'
import { HealthCheckPage } from './pages/HealthCheckPage'

type View = 'hardware' | 'health'

const VIEWS: { id: View; label: string }[] = [
  { id: 'hardware', label: 'Hardware Overview' },
  { id: 'health', label: 'Health Check' },
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
      {view === 'hardware' ? <HardwareOverviewPage /> : <HealthCheckPage />}
    </>
  )
}

export default App
