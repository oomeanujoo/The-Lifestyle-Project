import { apiGet, apiPost } from '../../lib/apiClient'

export type MasterRefreshOutcome = {
  masterName: string
  status: 'SUCCESS' | 'PARTIAL' | 'FAILED'
  recordsUpserted: number
  errorMessage: string | null
}

export type MasterStatus = {
  masterName: string
  recordCount: number
  lastRefreshedAt: string | null
  lastStatus: string | null
}

export type Section = { id: 'travel' | 'property'; label: string; basePath: string }

// The two sections a refresh covers — everything else on this page (status
// rows, currency list) comes from calling these endpoints, not from a
// hardcoded assumption about which masters exist.
export const SECTIONS: Section[] = [
  { id: 'travel', label: 'Travel', basePath: '/api/travel/v1/masters' },
  { id: 'property', label: 'Property', basePath: '/api/property/v1/masters' },
]

export const fetchStatus = (section: Section) => apiGet<MasterStatus[]>(`${section.basePath}/refresh-status`)

export const triggerRefresh = (section: Section) => apiPost<MasterRefreshOutcome[]>(`${section.basePath}/refresh`)

export const fetchCurrencies = (section: Section) => apiGet<string[]>(`${section.basePath}/currencies`)
