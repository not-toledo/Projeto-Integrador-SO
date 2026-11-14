import { describe, expect, it } from 'vitest'
import { bytes, count, dateTime, duration, frequency, NOT_AVAILABLE, percent, text } from './format'

describe('format', () => {
  it('text trata ausente e vazio como não disponível', () => {
    expect(text(null)).toBe(NOT_AVAILABLE)
    expect(text(undefined)).toBe(NOT_AVAILABLE)
    expect(text('   ')).toBe(NOT_AVAILABLE)
    expect(text('Intel')).toBe('Intel')
  })

  it('bytes converte unidades e rejeita valores inválidos', () => {
    expect(bytes(17_179_869_184)).toBe('16,00 GiB')
    expect(bytes(536_870_912)).toBe('512,00 MiB')
    expect(bytes(2 * 1024 ** 4)).toBe('2,00 TiB')
    expect(bytes(null)).toBe(NOT_AVAILABLE)
    expect(bytes(-1)).toBe(NOT_AVAILABLE)
    expect(bytes(Number.NaN)).toBe(NOT_AVAILABLE)
  })

  it('frequency mostra GHz ou MHz e trata zero como não disponível', () => {
    expect(frequency(3_600_000_000)).toBe('3,60 GHz')
    expect(frequency(800_000_000)).toBe('800 MHz')
    expect(frequency(0)).toBe(NOT_AVAILABLE)
    expect(frequency(null)).toBe(NOT_AVAILABLE)
  })

  it('percent aceita 0 a 100 e rejeita o resto', () => {
    expect(percent(42.5)).toBe('42,5%')
    expect(percent(0)).toBe('0,0%')
    expect(percent(100.5)).toBe(NOT_AVAILABLE)
    expect(percent(-1)).toBe(NOT_AVAILABLE)
    expect(percent(null)).toBe(NOT_AVAILABLE)
  })

  it('duration e count formatam ou indicam indisponibilidade', () => {
    expect(duration(93_780)).toBe('1d 2h 3min')
    expect(duration(null)).toBe(NOT_AVAILABLE)
    expect(count(8)).toBe('8')
    expect(count(null)).toBe(NOT_AVAILABLE)
  })

  it('dateTime rejeita datas inválidas', () => {
    expect(dateTime('2026-01-01T12:00:00Z')).not.toBe(NOT_AVAILABLE)
    expect(dateTime('não é data')).toBe(NOT_AVAILABLE)
    expect(dateTime(null)).toBe(NOT_AVAILABLE)
  })
})
