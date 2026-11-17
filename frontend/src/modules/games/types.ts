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
