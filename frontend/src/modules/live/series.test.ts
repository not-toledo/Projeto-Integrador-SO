import { describe, expect, it } from 'vitest'
import { makeSample, makeSampleWithoutSensors, makeStatus } from './fixtures'
import {
  MAX_POINTS,
  appendSample,
  buildSegments,
  connectionState,
  cpuTemperature,
  cpuTotal,
  driveBusy,
  driveNames,
  driveRateMiB,
  hasReadings,
  limitSamples,
  memoryPercent,
  niceMax,
  segmentPath,
} from './series'

const at = (seconds: number) => new Date(Date.UTC(2026, 10, 14, 12, 0, seconds)).toISOString()

describe('appendSample / limitSamples', () => {
  it('mantém no máximo MAX_POINTS amostras, descartando as mais antigas', () => {
    let samples = [] as ReturnType<typeof makeSample>[]
    for (let i = 0; i < MAX_POINTS + 25; i++) {
      samples = appendSample(samples, makeSample({ at: at(i) }))
    }
    expect(samples.length).toBe(MAX_POINTS)
    expect(samples[0].collectedAt).toBe(at(25))
    expect(samples[samples.length - 1].collectedAt).toBe(at(MAX_POINTS + 24))
  })

  it('ignora a mesma amostra recebida duas vezes', () => {
    const first = appendSample([], makeSample({ at: at(1) }))
    const again = appendSample(first, makeSample({ at: at(1) }))
    expect(again.length).toBe(1)
  })

  it('recomeça o histórico quando a amostra é mais antiga (backend reiniciado)', () => {
    const before = appendSample(appendSample([], makeSample({ at: at(10) })), makeSample({ at: at(12) }))
    const after = appendSample(before, makeSample({ at: at(1) }))
    expect(after.length).toBe(1)
    expect(after[0].collectedAt).toBe(at(1))
  })

  it('ignora amostra com horário inválido', () => {
    const samples = appendSample([], makeSample({ at: at(1) }))
    expect(appendSample(samples, makeSample({ at: 'não é data' })).length).toBe(1)
  })

  it('limita um histórico recebido da API', () => {
    const many = Array.from({ length: 5 }, (_, i) => makeSample({ at: at(i) }))
    expect(limitSamples(many, 3).map((s) => s.collectedAt)).toEqual([at(2), at(3), at(4)])
    expect(limitSamples(many, 10).length).toBe(5)
  })
})

describe('extratores', () => {
  it('leem os valores válidos de uma amostra completa', () => {
    const s = makeSample()
    expect(cpuTotal(s)).toBe(25)
    expect(memoryPercent(s)).toBe(62.5)
    expect(cpuTemperature(s)).toBe(55)
    expect(driveRateMiB(s, 'Disco de teste', 'read')).toBeCloseTo(2, 5)
    expect(driveRateMiB(s, 'Disco de teste', 'write')).toBeCloseTo(0.5, 5)
    expect(driveBusy(s, 'Disco de teste')).toBe(15)
  })

  it('devolvem null (sem leitura) quando o grupo está indisponível ou o campo é nulo', () => {
    const s = makeSampleWithoutSensors()
    expect(cpuTemperature(s)).toBeNull()
    expect(driveRateMiB(s, 'Disco de teste', 'read')).toBeNull()
    expect(driveBusy(s, 'Disco de teste')).toBeNull()
    expect(driveRateMiB(s, 'Outra unidade', 'read')).toBeNull()
  })

  it('descartam valores inválidos: negativos, NaN e percentuais acima de 100', () => {
    const bad = makeSample({
      cpu: { status: 'OK', message: null, data: { totalPercent: 130, perCorePercent: null } },
      memory: { status: 'OK', message: null, data: { totalBytes: 1, availableBytes: 1, usedBytes: 1, usedPercent: -5 } },
      sensors: { status: 'OK', message: null, data: { cpuTemperatureCelsius: Number.NaN, fanSpeedsRpm: null } },
    })
    expect(cpuTotal(bad)).toBeNull()
    expect(memoryPercent(bad)).toBeNull()
    expect(cpuTemperature(bad)).toBeNull()
  })

  it('não leem dados de um grupo com ERROR', () => {
    const s = makeSample({ cpu: { status: 'ERROR', message: 'falhou', data: null } })
    expect(cpuTotal(s)).toBeNull()
  })

  it('driveNames usa a amostra mais recente que informou unidades', () => {
    const withDrives = makeSample({ at: at(1) })
    const without = makeSample({ at: at(2), disks: { status: 'UNAVAILABLE', message: null, data: null } })
    expect(driveNames([withDrives, without])).toEqual(['Disco de teste'])
    expect(driveNames([without])).toEqual([])
    expect(driveNames([])).toEqual([])
  })

  it('hasReadings detecta se há ao menos uma leitura válida', () => {
    expect(hasReadings([null, null])).toBe(false)
    expect(hasReadings([null, 0])).toBe(true)
    expect(hasReadings([])).toBe(false)
  })
})

describe('niceMax', () => {
  it('arredonda para 1, 2 ou 5 vezes uma potência de 10', () => {
    expect(niceMax(0.3)).toBe(0.5)
    expect(niceMax(1)).toBe(1)
    expect(niceMax(1.2)).toBe(2)
    expect(niceMax(3.7)).toBe(5)
    expect(niceMax(7)).toBe(10)
    expect(niceMax(84)).toBe(100)
    expect(niceMax(130)).toBe(200)
  })

  it('usa 1 quando não há valor positivo', () => {
    expect(niceMax(0)).toBe(1)
    expect(niceMax(-3)).toBe(1)
    expect(niceMax(Number.NaN)).toBe(1)
  })
})

describe('buildSegments / segmentPath', () => {
  const x = (t: number) => t
  const y = (v: number) => v

  it('une pontos consecutivos em um único segmento', () => {
    expect(buildSegments([0, 1, 2], [10, 20, 30], x, y)).toEqual([
      [
        { x: 0, y: 10 },
        { x: 1, y: 20 },
        { x: 2, y: 30 },
      ],
    ])
  })

  it('um valor sem leitura abre uma lacuna (não liga os pontos nem usa zero)', () => {
    const segments = buildSegments([0, 1, 2, 3], [10, null, null, 40], x, y)
    expect(segments.length).toBe(2)
    expect(segments[0]).toEqual([{ x: 0, y: 10 }])
    expect(segments[1]).toEqual([{ x: 3, y: 40 }])
  })

  it('interrompe a linha quando o intervalo entre pontos excede o máximo', () => {
    const segments = buildSegments([0, 1000, 2000, 60_000, 61_000], [1, 2, 3, 4, 5], x, y, 5000)
    expect(segments.map((s) => s.length)).toEqual([3, 2])
  })

  it('série inteira sem leitura não gera segmentos', () => {
    expect(buildSegments([0, 1], [null, null], x, y)).toEqual([])
    expect(buildSegments([], [], x, y)).toEqual([])
  })

  it('ignora valores inválidos (negativo, NaN)', () => {
    expect(buildSegments([0, 1, 2], [5, -1, Number.NaN], x, y).length).toBe(1)
  })

  it('segmentPath gera o atributo d e torna visível um ponto isolado', () => {
    expect(
      segmentPath([
        { x: 0, y: 1 },
        { x: 2, y: 3.5 },
      ]),
    ).toBe('M0.00 1.00 L2.00 3.50')
    expect(segmentPath([{ x: 4, y: 5 }])).toBe('M4.00 5.00h0.01')
    expect(segmentPath([])).toBe('')
  })
})

describe('connectionState', () => {
  const now = Date.parse('2026-11-14T12:00:10Z')
  const base = {
    everConnected: true,
    clientFailures: 0,
    status: makeStatus(),
    latest: makeSample({ at: '2026-11-14T12:00:09Z' }),
    now,
  }

  it('conectando antes da primeira resposta', () => {
    expect(connectionState({ ...base, everConnected: false, status: null, latest: null })).toBe('connecting')
  })

  it('aguardando quando o backend responde mas ainda não há amostra', () => {
    expect(connectionState({ ...base, latest: null })).toBe('waiting')
  })

  it('conectado com amostra recente', () => {
    expect(connectionState(base)).toBe('connected')
  })

  it('sem conexão após falhas de comunicação (mesmo com dados antigos em tela)', () => {
    expect(connectionState({ ...base, clientFailures: 2 })).toBe('disconnected')
    expect(connectionState({ ...base, everConnected: false, status: null, latest: null, clientFailures: 1 })).toBe('disconnected')
  })

  it('coleta com problemas quando travada ou com falhas seguidas', () => {
    expect(connectionState({ ...base, status: makeStatus({ stalled: true }) })).toBe('degraded')
    expect(connectionState({ ...base, status: makeStatus({ consecutiveFailures: 3, lastError: 'x' }) })).toBe('degraded')
  })

  it('sem amostras novas quando a última é muito antiga', () => {
    const old = makeSample({ at: '2026-11-14T11:59:00Z' })
    expect(connectionState({ ...base, latest: old })).toBe('stale')
  })

  it('tolera atraso normal de poucas coletas', () => {
    const recent = makeSample({ at: '2026-11-14T12:00:03Z' })
    expect(connectionState({ ...base, latest: recent })).toBe('connected')
  })
})
