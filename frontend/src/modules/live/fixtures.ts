// Dados FICTÍCIOS usados somente pelos testes automatizados. Nunca são exibidos pela aplicação.
import type { ComponentResult } from '../hardware/types'
import type { LiveHistoryResponse, LiveResponse, LiveSample, MonitorStatus } from './types'

const OK = <T>(data: T): ComponentResult<T> => ({ status: 'OK', message: null, data })

export const GPU_UNAVAILABLE: LiveSample['gpu'] = {
  status: 'UNAVAILABLE',
  message: 'O leitor de sensores (PCCheck.SensorHost.exe) não foi encontrado.',
  data: null,
}

/** GPU com leituras (valores de teste). */
export const GPU_OK: LiveSample['gpu'] = {
  status: 'OK',
  message: null,
  data: {
    utilizationPercent: 42,
    vramUsedBytes: 2 * 1024 ** 3,
    vramTotalBytes: 8 * 1024 ** 3,
    temperatureCelsius: 61,
    name: 'GPU de teste',
  },
}

export function makeStatus(overrides: Partial<MonitorStatus> = {}): MonitorStatus {
  return {
    running: true,
    collecting: false,
    stalled: false,
    intervalMs: 2000,
    historyCapacity: 300,
    sampleCount: 1,
    lastSuccessAt: null,
    consecutiveFailures: 0,
    lastError: null,
    ...overrides,
  }
}

export function makeSample(overrides: Partial<LiveSample> & { at?: string } = {}): LiveSample {
  const { at, ...rest } = overrides
  return {
    collectedAt: at ?? '2026-11-14T12:00:00Z',
    durationMs: 12,
    cpu: OK({ totalPercent: 25, perCorePercent: [20, 30, null, 28] }),
    memory: OK({ totalBytes: 16 * 1024 ** 3, availableBytes: 6 * 1024 ** 3, usedBytes: 10 * 1024 ** 3, usedPercent: 62.5 }),
    gpu: GPU_UNAVAILABLE,
    disks: OK({
      drives: [{ name: 'Disco de teste', readBytesPerSecond: 2 * 1024 ** 2, writeBytesPerSecond: 512 * 1024, busyPercent: 15 }],
      volumes: [
        { mountPoint: 'C:', label: null, fileSystem: 'NTFS', totalBytes: 500 * 1024 ** 3, freeBytes: 200 * 1024 ** 3, usedBytes: 300 * 1024 ** 3, usedPercent: 60 },
      ],
    }),
    sensors: OK({ cpuTemperatureCelsius: 55, fanSpeedsRpm: [1200, 1300] }),
    ...rest,
  }
}

/** Amostra de um computador sem sensores nem taxas de disco disponíveis. */
export function makeSampleWithoutSensors(at = '2026-11-14T12:00:02Z'): LiveSample {
  return makeSample({
    at,
    sensors: { status: 'UNAVAILABLE', message: 'Nenhum sensor exposto pelo sistema.', data: null },
    disks: OK({
      drives: [{ name: 'Disco de teste', readBytesPerSecond: null, writeBytesPerSecond: null, busyPercent: null }],
      volumes: null,
    }),
  })
}

export function makeLive(latest: LiveSample | null, status: Partial<MonitorStatus> = {}): LiveResponse {
  return { source: 'fonte de teste', status: makeStatus(status), latest }
}

export function makeHistory(samples: LiveSample[], status: Partial<MonitorStatus> = {}): LiveHistoryResponse {
  return { source: 'fonte de teste', status: makeStatus({ sampleCount: samples.length, ...status }), samples }
}
