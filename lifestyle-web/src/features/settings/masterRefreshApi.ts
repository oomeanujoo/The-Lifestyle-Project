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

const BASE_PATH = '/api/integration/v1/masters/lifestyle'

// Integration owns the shared masters; Settings never refreshes domain copies.
export const fetchStatus = () => apiGet<MasterStatus[]>(`${BASE_PATH}/refresh-status`)

export const triggerRefresh = () => apiPost<MasterRefreshOutcome[]>(`${BASE_PATH}/refresh`)

export const fetchCurrencies = () => apiGet<string[]>(`${BASE_PATH}/currencies`)
