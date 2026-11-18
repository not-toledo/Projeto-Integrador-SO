import type { ReactNode } from 'react'
import { ArrowIcon, ChipIcon, GamepadIcon, HealthIcon, HistoryIcon, PulseIcon } from '../components/Icons'
import { ArchitectureStack } from '../components/illustrations/ArchitectureStack'
import { IsoChip } from '../components/illustrations/IsoChip'
import { REPOSITORY_URL } from '../components/SiteFooter'
import { LiveGlance } from '../modules/home/LiveGlance'
import { SystemSummary } from '../modules/home/SystemSummary'
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

const STEPS: { title: string; text: string }[] = [
  { title: 'Pesquise o jogo', text: 'Pelo nome, na loja da Steam, ou na lista dos jogos já instalados.' },
  { title: 'Leia os requisitos', text: 'Mínimos e recomendados, como o jogo publica: processador, placa de vídeo, memória e espaço.' },
  {
    title: 'Compare',
    text: 'Com o hardware lido deste PC. Modelos diferentes de CPU e GPU são comparados pelo benchmark do Blender Open Data.',
  },
  { title: 'Veja o resultado', text: 'Item a item e no geral, com o motivo de cada conclusão.' },
]

/** Classificações gerais que o Módulo D pode dar (as mesmas do backend). */
const VERDICTS: { label: string; cls: string }[] = [
  { label: 'Compatível', cls: 'game-status-ok' },
  { label: 'Compatibilidade limitada', cls: 'game-status-unknown' },
  { label: 'Não recomendado', cls: 'game-status-bad' },
  { label: 'Dados insuficientes', cls: '' },
]

const PRINCIPLES: { title: string; text: string }[] = [
  { title: 'Dados reais', text: 'Tudo é lido do próprio computador. Nada é simulado ou estimado.' },
  {
    title: 'Ausência explicada',
    text: '"Não disponível" (o sistema não informou) é diferente de falha de coleta, e nenhum dos dois vira defeito.',
  },
  { title: 'Local', text: 'A API só aceita conexões deste computador (127.0.0.1). Só a pesquisa de jogos usa a internet.' },
  { title: 'Somente leitura', text: 'O PCCheck observa o sistema. Ele não muda configurações nem encerra programas.' },
]

function scrollToFeatures() {
  document.getElementById('funcionalidades')?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

/** Início: o que é o PCCheck, um resumo real deste computador e o acesso a cada módulo. */
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

      <SystemSummary />

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

      <LiveGlance />

      <section className="section split" aria-labelledby="games-title">
        <div>
          <h2 id="games-title" className="section-title display">Vai rodar?</h2>
          <p className="section-lead">
            Antes de comprar ou instalar, compare os requisitos de um jogo com este computador.
          </p>
          <ol className="steps">
            {STEPS.map((s, i) => (
              <li key={s.title} className="step">
                <span className="step-number display" aria-hidden="true">{String(i + 1).padStart(2, '0')}</span>
                <div>
                  <h3 className="step-title">{s.title}</h3>
                  <p className="step-text">{s.text}</p>
                </div>
              </li>
            ))}
          </ol>
          <a className="btn btn-primary" href={hrefOf('games')}>Verificar um jogo <ArrowIcon /></a>
        </div>
        <aside className="verdict-legend" aria-label="Classificações possíveis">
          <p className="verdict-legend-title">Classificações possíveis</p>
          <ul>
            {VERDICTS.map((v) => (
              <li key={v.label}><span className={`game-status ${v.cls}`}>{v.label}</span></li>
            ))}
          </ul>
          <p className="muted">
            O resultado compara requisitos publicados com o hardware. Ele não garante FPS nem qualidade gráfica.
          </p>
        </aside>
      </section>

      <section className="section split" aria-labelledby="about-title">
        <div>
          <h2 id="about-title" className="section-title display">Sobre o projeto</h2>
          <p className="section-lead">
            Saber o que existe dentro do computador, se ele está saudável e se aguenta um jogo costuma exigir várias
            ferramentas e conhecimento técnico. O PCCheck reúne essas respostas numa interface só, explicando de onde
            vem cada dado. É o Projeto Integrador da disciplina de Sistemas Operacionais.
          </p>
          <dl className="principles">
            {PRINCIPLES.map((p) => (
              <div key={p.title} className="principle">
                <dt>{p.title}</dt>
                <dd>{p.text}</dd>
              </div>
            ))}
          </dl>
          <a className="link-arrow" href={REPOSITORY_URL} target="_blank" rel="noreferrer">
            Código no GitHub <ArrowIcon />
          </a>
        </div>
        <figure className="architecture">
          <ArchitectureStack className="architecture-art" />
          <figcaption className="muted">Camadas do PCCheck: a interface fala com a API local, que lê o sistema.</figcaption>
        </figure>
      </section>
    </main>
  )
}
