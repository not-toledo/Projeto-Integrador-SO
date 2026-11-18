import type { ReactNode } from 'react'
import { ArrowIcon, ChipIcon, GamepadIcon, HealthIcon, HistoryIcon, PulseIcon } from '../components/Icons'
import { IsoChip } from '../components/illustrations/IsoChip'
import { hrefOf, type View } from '../navigation'

const FEATURES: { view: View; name: string; icon: ReactNode; text: string }[] = [
  {
    view: 'hardware', name: 'Hardware', icon: <ChipIcon />,
    text: 'Sistema operacional, processador, memória, placas de vídeo, placa-mãe e discos, com a origem de cada dado.',
  },
  {
    view: 'health', name: 'Saúde', icon: <HealthIcon />,
    text: 'Temperatura, espaço em disco, uso de memória e saúde SMART dos discos, com alertas explicados.',
  },
  {
    view: 'live', name: 'Monitor', icon: <PulseIcon />,
    text: 'CPU, RAM, GPU, sensores e discos em tempo real, com gráficos dos últimos minutos.',
  },
  {
    view: 'history', name: 'Histórico', icon: <HistoryIcon />,
    text: 'Coletas agendadas pelo sistema operacional, mesmo com o PCCheck fechado, por até 7 dias.',
  },
  {
    view: 'games', name: 'Jogos', icon: <GamepadIcon />,
    text: 'Requisitos da Steam comparados ao seu hardware, jogos instalados e pré-checagem antes de jogar.',
  },
]

function scrollToFeatures() {
  document.getElementById('funcionalidades')?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

/** Início: o que é o PCCheck e o acesso a cada módulo. */
export function HomePage() {
  return (
    <main className="home">
      <section className="hero section" aria-labelledby="hero-title">
        <div className="hero-text">
          <p className="eyebrow">Monitoramento local de hardware</p>
          <h1 id="hero-title" className="display hero-title">
            Conheça<br />seu<br />hardware
          </h1>
          <p className="hero-lead">
            O PCCheck identifica os componentes, acompanha a saúde e o uso do computador em tempo real e compara o seu
            hardware com os requisitos dos jogos. Tudo lido do próprio sistema, sem valores simulados.
          </p>
          <div className="actions">
            <a className="btn btn-primary" href={hrefOf('live')}>Abrir monitoramento <ArrowIcon /></a>
            <button type="button" className="btn" onClick={scrollToFeatures}>Ver funcionalidades</button>
          </div>
        </div>
        <IsoChip className="hero-art" />
      </section>

      <section id="funcionalidades" className="section" aria-labelledby="features-title">
        <h2 id="features-title" className="section-title display">Funcionalidades</h2>
        <ul className="features">
          {FEATURES.map((f) => (
            <li key={f.view} className="feature">
              <span className="feature-icon">{f.icon}</span>
              <h3 className="feature-name">{f.name}</h3>
              <p className="feature-text">{f.text}</p>
              <a className="link-arrow" href={hrefOf(f.view)} aria-label={`Abrir ${f.name}`}>
                Abrir <ArrowIcon />
              </a>
            </li>
          ))}
        </ul>
      </section>
    </main>
  )
}
