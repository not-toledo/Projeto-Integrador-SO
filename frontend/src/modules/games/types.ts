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
}
