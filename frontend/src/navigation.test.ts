import { describe, expect, it } from 'vitest'
import { hrefOf, viewFromHash } from './navigation'

describe('navigation', () => {
  it('lê a tela do fragmento do endereço', () => {
    expect(viewFromHash('#/live')).toBe('live')
    expect(viewFromHash('#/games?x=1')).toBe('games')
    expect(viewFromHash('')).toBe('home')
    expect(viewFromHash('#/')).toBe('home')
    expect(viewFromHash('#/desconhecida')).toBe('home')
    expect(viewFromHash('#conteudo')).toBe('home')
  })

  it('gera o endereço de cada tela', () => {
    expect(hrefOf('home')).toBe('#/')
    expect(hrefOf('history')).toBe('#/history')
    expect(viewFromHash(hrefOf('health'))).toBe('health')
  })
})
