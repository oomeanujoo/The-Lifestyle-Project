import { useEffect, useState } from 'react'
import { fetchAiProviderStatus, type AiProviderStatus } from './aiStatusApi'

// Color meaning, per the explicit design: green = last real call to this
// provider succeeded; red = last real call failed, was rate-limited, or hit
// an open circuit breaker; orange = not configured yet, or never called
// this run. Never a live ping — see TECHNICAL_ARCHITECTURE.md §18.
const dotColor = (provider: AiProviderStatus) => {
  if (!provider.configured) return 'bg-amber-400'
  if (provider.health === 'UP') return 'bg-green-500'
  if (provider.health === 'DOWN') return 'bg-red-500'
  return 'bg-amber-400'
}

const statusLabel = (provider: AiProviderStatus) => {
  if (!provider.configured) return 'Not configured'
  if (provider.health === 'UP') return 'Last call succeeded'
  if (provider.health === 'DOWN') return provider.lastError ? `Last call failed — ${provider.lastError}` : 'Last call failed'
  return 'Not called yet'
}

// Sits below "Last checked," kept to one compact row per the Dashboard's
// own one-viewport rule (§15.4) — a status dot per AI model, not a panel.
export function AiStatusWidget() {
  const [providers, setProviders] = useState<AiProviderStatus[] | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    fetchAiProviderStatus()
      .then(setProviders)
      .catch(() => setError(true))
  }, [])

  if (error) return null // integration-service isn't reachable — fail quiet, this is a minor widget, not core content
  if (!providers) return null

  return (
    <section aria-labelledby="ai-status-title" className="mt-8">
      <div className="mb-2.5 flex items-baseline justify-between">
        <h2 id="ai-status-title" className="font-display text-[1.1rem] font-semibold">AI models</h2>
        <span className="text-[.8rem] text-muted-600">via integration-service</span>
      </div>
      <div className="flex flex-wrap gap-3">
        {providers.map(provider => (
          <div
            key={provider.name}
            title={statusLabel(provider)}
            className="flex items-center gap-2 rounded-full border border-line bg-white px-3.5 py-1.5 text-[.85rem] text-ink"
          >
            <span className={`size-2.5 rounded-full ${dotColor(provider)}`} aria-hidden="true" />
            <span className="font-semibold capitalize">{provider.name}</span>
          </div>
        ))}
      </div>
    </section>
  )
}
