import { hrefOf } from '../navigation'

export const REPOSITORY_URL = 'https://github.com/not-toledo/Projeto-Integrador-SO'

/** Rodapé: atalhos, repositório e créditos das bibliotecas e fontes de dados. */
export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="site-footer-inner">
        <nav className="footer-links" aria-label="Atalhos">
          <a href={hrefOf('hardware')}>Hardware</a>
          <a href={hrefOf('live')}>Monitor</a>
          <a href={hrefOf('games')}>Jogos</a>
          <a href={REPOSITORY_URL} target="_blank" rel="noreferrer">GitHub</a>
        </nav>
        <p className="footer-about">
          PCCheck · Projeto Integrador de Sistemas Operacionais
        </p>
        <p className="footer-credits">
          Dados: OSHI · LibreHardwareMonitor · Steam · Blender Open Data (CC0) · Fonte Silkscreen (OFL)
        </p>
      </div>
    </footer>
  )
}
