import { apiGet } from '../../lib/apiClient'

export type AiProviderStatus = {
  name: string
  configured: boolean
  health: 'UP' | 'DOWN' | 'UNKNOWN'
  lastCheckedAt: string | null
  lastError: string | null
}

// integration-service decides health from the outcome of the last real
// call it made to a provider, updated on every subsequent call — this is
// never a fresh ping, so calling this endpoint costs no AI-provider quota.
// See TECHNICAL_ARCHITECTURE.md §18.
export const fetchAiProviderStatus = () => apiGet<AiProviderStatus[]>('/api/integration/v1/ai/providers/status')
