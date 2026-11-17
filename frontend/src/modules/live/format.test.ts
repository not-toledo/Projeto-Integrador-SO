import { describe, expect, it } from 'vitest'
import { NO_READING, celsius, intervalLabel, isPercentReading, isReading, liveBytes, livePercent, rate, rpm } from './format'

describe('formatadores do Live Monitor', () => {
  it('valores ausentes ou inválidos viram "Sem leitura"', () => {
    for (const bad of [null, undefined, Number.NaN, -1, Number.POSITIVE_INFINITY]) {
      expect(livePercent(bad)).toBe(NO_READING)
      expect(celsius(bad)).toBe(NO_READING)
      expect(rpm(bad)).toBe(NO_READING)
      expect(liveBytes(bad)).toBe(NO_READING)
      expect(rate(bad)).toBe(NO_READING)
    }
    expect(livePercent(100.1)).toBe(NO_READING)
  })

  it('formata percentuais, temperatura e rotação em pt-BR', () => {
    expect(livePercent(62.5)).toBe('62,5%')
    expect(livePercent(0)).toBe('0,0%')
    expect(celsius(55)).toBe('55,0 °C')
    expect(rpm(1200)).toBe('1.200 RPM')
  })

  it('formata bytes e taxas na unidade adequada', () => {
    expect(liveBytes(512 * 1024 ** 2)).toBe('512,00 MiB')
    expect(liveBytes(16 * 1024 ** 3)).toBe('16,00 GiB')
    expect(rate(512 * 1024)).toBe('512,0 KiB/s')
    expect(rate(2 * 1024 ** 2)).toBe('2,00 MiB/s')
    expect(rate(0)).toBe('0,0 KiB/s')
  })

  it('intervalLabel', () => {
    expect(intervalLabel(2000)).toBe('2 s')
    expect(intervalLabel(500)).toBe('0,5 s')
  })

  it('validadores', () => {
    expect(isReading(0)).toBe(true)
    expect(isReading(-0.1)).toBe(false)
    expect(isPercentReading(100)).toBe(true)
    expect(isPercentReading(101)).toBe(false)
  })
})
