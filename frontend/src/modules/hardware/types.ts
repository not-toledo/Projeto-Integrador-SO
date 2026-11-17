// Tipos que espelham a resposta de GET /api/inventory (veja docs/api-inventario.md).
// Em qualquer campo, null significa "não disponível".

export type ComponentStatus = 'OK' | 'UNAVAILABLE' | 'ERROR'

export interface ComponentResult<T> {
  status: ComponentStatus
  message: string | null
  data: T | null
}

export interface OsInfo {
  name: string | null
  version: string | null
  buildNumber: string | null
  architecture: string | null
  uptimeSeconds: number | null
}

export interface CpuInfo {
  name: string | null
  vendor: string | null
  physicalCores: number | null
  logicalProcessors: number | null
  nominalFrequencyHz: number | null
  maxFrequencyHz: number | null
  usagePercent: number | null
}

export interface MemoryInfo {
  totalBytes: number | null
  availableBytes: number | null
  usedBytes: number | null
  usedPercent: number | null
}

export interface GpuInfo {
  name: string | null
  vendor: string | null
  deviceId: string | null
  vramBytes: number | null
}

export interface MotherboardInfo {
  manufacturer: string | null
  model: string | null
  version: string | null
  serialNumber: string | null
  systemManufacturer: string | null
  systemModel: string | null
  /** Campos que o firmware deixou com texto genérico do fabricante (ex.: "To be filled by O.E.M."). */
  notReportedFields?: MotherboardField[]
}

export type MotherboardField =
  | 'manufacturer' | 'model' | 'version' | 'serialNumber' | 'systemManufacturer' | 'systemModel'

export interface DiskInfo {
  name: string | null
  model: string | null
  sizeBytes: number | null
}

export interface VolumeInfo {
  mountPoint: string | null
  label: string | null
  fileSystem: string | null
  totalBytes: number | null
  freeBytes: number | null
  usedBytes: number | null
  usedPercent: number | null
}

export interface StorageInfo {
  drives: DiskInfo[] | null
  volumes: VolumeInfo[] | null
}

export interface Inventory {
  collectedAt: string
  source: string
  os: ComponentResult<OsInfo>
  cpu: ComponentResult<CpuInfo>
  memory: ComponentResult<MemoryInfo>
  gpu: ComponentResult<GpuInfo[]>
  motherboard: ComponentResult<MotherboardInfo>
  storage: ComponentResult<StorageInfo>
}
