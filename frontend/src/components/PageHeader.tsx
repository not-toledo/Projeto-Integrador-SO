import type { ReactNode } from 'react'

/** Cabeçalho das telas internas: módulo, título, informações da coleta e ações (ex.: Atualizar). */
export function PageHeader({ eyebrow, title, children, actions }: {
  eyebrow: string
  title: string
  children?: ReactNode
  actions?: ReactNode
}) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {children}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  )
}
