import { describe, expect, it } from 'vitest'
import { makeAlert } from './fixtures'
import { KIND_LABEL, SEVERITY_LABEL, overallMessage, sortAlerts } from './labels'

describe('labels', () => {
  it('usa os nomes padronizados de severidade', () => {
    expect(SEVERITY_LABEL).toEqual({ INFO: 'Informação', WARNING: 'Atenção', CRITICAL: 'Crítico' })
  })

  it('deixa claro que alerta preventivo e dado indisponível não comprovam defeito', () => {
    expect(KIND_LABEL.PREVENTIVE).toMatch(/não comprova defeito/)
    expect(KIND_LABEL.UNAVAILABLE).toMatch(/não é falha física/)
  })

  it('resume o nível geral sem afirmar defeito', () => {
    expect(overallMessage('INFO', 0, 0)).toMatch(/Nenhum indicador/)
    expect(overallMessage('WARNING', 0, 2)).toMatch(/2 indicador/)
    expect(overallMessage('CRITICAL', 1, 0)).toMatch(/preventivos/)
  })

  it('ordena do mais grave para o menos grave sem alterar a lista original', () => {
    const original = [
      makeAlert({ id: 'a', severity: 'INFO' }),
      makeAlert({ id: 'b', severity: 'CRITICAL' }),
      makeAlert({ id: 'c', severity: 'WARNING' }),
    ]
    expect(sortAlerts(original).map((a) => a.id)).toEqual(['b', 'c', 'a'])
    expect(original.map((a) => a.id)).toEqual(['a', 'b', 'c'])
  })
})
