import { describe, expect, it } from 'vitest'
import { duelHeadline, duelResult } from './duel'

describe('duelo de benchmark', () => {
  it('diz quem é melhor e por quanto', () => {
    expect(duelResult(0.94)).toBe('required')
    expect(duelHeadline(0.94, 'CPU')).toBe('O exigido é melhor: o seu processador rende 94%')
    expect(duelHeadline(1.62, 'CPU')).toBe('O seu processador é melhor: 62% mais rápido')
    expect(duelHeadline(1.82, 'GPU')).toBe('A sua placa é melhor: 82% mais rápido')
  })

  it('diferença menor que 3% é empate', () => {
    expect(duelResult(0.98)).toBe('tie')
    expect(duelResult(1.02)).toBe('tie')
    expect(duelHeadline(1.01, 'CPU')).toBe('Praticamente iguais')
  })
})
