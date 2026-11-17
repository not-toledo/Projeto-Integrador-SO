// Dados FICTÍCIOS usados apenas nos testes automatizados. Nunca são importados pela aplicação.
import type { Inventory } from './types'

export function makeInventory(overrides: Partial<Inventory> = {}): Inventory {
  return {
    collectedAt: '2026-01-01T12:00:00Z',
    source: 'Origem de teste',
    os: {
      status: 'OK',
      message: null,
      data: { name: 'SO de teste', version: '1.0', buildNumber: null, architecture: 'amd64', uptimeSeconds: 3600 },
    },
    cpu: {
      status: 'OK',
      message: null,
      data: {
        name: 'CPU de teste',
        vendor: null,
        physicalCores: 4,
        logicalProcessors: 8,
        nominalFrequencyHz: 3_000_000_000,
        maxFrequencyHz: null,
        usagePercent: 25,
      },
    },
    memory: {
      status: 'OK',
      message: null,
      data: { totalBytes: 8_589_934_592, availableBytes: 4_294_967_296, usedBytes: 4_294_967_296, usedPercent: 50 },
    },
    gpu: {
      status: 'OK',
      message: null,
      data: [{ name: 'GPU de teste', vendor: null, deviceId: null, vramBytes: 2_147_483_648 }],
    },
    motherboard: {
      status: 'OK',
      message: null,
      data: {
        manufacturer: 'Placa de teste',
        model: null,
        version: null,
        serialNumber: null,
        systemManufacturer: null,
        systemModel: null,
      },
    },
    storage: {
      status: 'OK',
      message: null,
      data: {
        drives: [{ name: 'disco-teste', model: null, sizeBytes: 500_000_000_000 }],
        volumes: null,
      },
    },
    ...overrides,
  }
}
