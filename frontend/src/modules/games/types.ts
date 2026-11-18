export interface GameSearchResult {
  appId: number
  name: string
  headerImage: string | null
  storeUrl: string
}
export interface ComponentCompatibility {
  component: string
  requirement: string
  detected: string
  status: 'COMPATIBLE' | 'BELOW_MINIMUM' | 'UNKNOWN' | 'MANUAL_REVIEW'
  explanation: string
}
export interface GameCompatibility {
  appId: number
  gameName: string
  overallMinimum: string
  overallRecommended: string
  minimumResults: ComponentCompatibility[]
  recommendedResults: ComponentCompatibility[]
  minimumRawText: string
  recommendedRawText: string
  evaluatedAt: string
  source: string
  warnings: string[]
  verdict: CompatibilityVerdict
}
/** Classificação geral: abaixo do mínimo comprovado, só mínimos, tudo confirmado, ou dados que faltam. */
export interface CompatibilityVerdict {
  code: 'COMPATIBLE' | 'LIMITED' | 'NOT_RECOMMENDED' | 'INSUFFICIENT_DATA'
  label: string
  reasons: string[]
  belowMinimum: string[]
  belowRecommended: string[]
  pendingReview: string[]
}

/** Biblioteca (pasta de jogos) da Steam e o volume/disco onde está. */
export interface SteamLibrary {
  path: string
  mountPoint: string | null
  freeBytes: number | null
  totalBytes: number | null
  /** "SSD"/"HDD" do disco físico do volume, só quando o SO informa. */
  mediaType: string | null
  available: boolean
  gameCount: number
}

export interface InstalledGame {
  appId: number
  name: string | null
  installDir: string | null
  libraryPath: string
  mountPoint: string | null
  mediaType: string | null
  sizeOnDiskBytes: number | null
  state: 'INSTALLED' | 'UPDATE_REQUIRED' | 'INCOMPLETE'
  lastUpdated: string | null
  storeUrl: string
}

export interface InstalledGames {
  steamFound: boolean
  steamPath: string | null
  libraries: SteamLibrary[]
  games: InstalledGame[]
  problems: string[]
  scannedAt: string
}

/** Uma verificação da pré-checagem; UNAVAILABLE = não verificado (nunca tratado como ok). */
export interface PrecheckItem {
  id: string
  label: string
  status: 'OK' | 'ATTENTION' | 'INFO' | 'UNAVAILABLE'
  observed: string | null
  detail: string
}

/** Processos com o mesmo nome somados; cpuPercent é % do total da CPU. */
export interface ProcessGroup {
  name: string
  processCount: number
  cpuPercent: number
  memoryBytes: number
}

export interface Precheck {
  appId: number
  gameName: string
  overall: 'READY' | 'ATTENTION'
  overallLabel: string
  items: PrecheckItem[]
  topCpu: ProcessGroup[]
  topMemory: ProcessGroup[]
  processError: string | null
  checkedAt: string
}
