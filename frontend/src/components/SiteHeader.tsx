import { useEffect, useState } from 'react'
import { VIEWS, hrefOf, type View } from '../navigation'
import { CloseIcon, MenuIcon } from './Icons'

/** Cabeçalho fixo: logotipo e navegação. Em telas estreitas, a navegação abre pelo botão MENU. */
export function SiteHeader({ current }: { current: View }) {
  const [open, setOpen] = useState(false)

  // Fecha o menu com Esc e ao escolher uma tela.
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <a className="logo" href={hrefOf('home')} aria-label="NEXCORE, início" onClick={() => setOpen(false)}>
          {/* Logotipo oficial (gerado por branding/gerar-recursos.ps1); o nome ao lado é o texto acessível. */}
          <img className="logo-mark" src="/brand/nexcore-64.png" srcSet="/brand/nexcore-64.png 1x, /brand/nexcore-128.png 2x"
            width={32} height={32} alt="" />
          NEXCORE
        </a>
        <button type="button" className="menu-toggle" aria-expanded={open} aria-controls="site-nav"
          onClick={() => setOpen((v) => !v)}>
          Menu {open ? <CloseIcon /> : <MenuIcon />}
        </button>
        <nav id="site-nav" className={open ? 'site-nav site-nav-open' : 'site-nav'} aria-label="Telas do NEXCORE">
          {VIEWS.map(({ id, label }) => (
            <a key={id} href={hrefOf(id)} className={id === current ? 'site-nav-link active' : 'site-nav-link'}
              aria-current={id === current ? 'page' : undefined} onClick={() => setOpen(false)}>
              {label}
            </a>
          ))}
        </nav>
      </div>
    </header>
  )
}
