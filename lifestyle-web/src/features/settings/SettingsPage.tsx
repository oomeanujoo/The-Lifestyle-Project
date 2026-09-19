import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowPathIcon } from '@heroicons/react/24/outline'
import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { DataRow } from '../../components/DataRow'
import { readCurrency, saveCurrency } from '../../lib/currencyPreference'
import { fetchStatus, triggerRefresh, fetchCurrencies, type MasterRefreshOutcome, type MasterStatus } from './masterRefreshApi'

const formatTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : 'Never refreshed')
const errorMessage = (error: unknown) => error instanceof Error ? error.message : 'Unexpected request failure'

// Integration supplies one shared master list and one refresh outcome list.
export function SettingsPage() {
  const [statuses, setStatuses] = useState<MasterStatus[]>([])
  const [statusLoading, setStatusLoading] = useState(true)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [currencies, setCurrencies] = useState<string[]>([])
  const [currencyLoading, setCurrencyLoading] = useState(true)
  const [currencyError, setCurrencyError] = useState<string | null>(null)
  const [selectedCurrency, setSelectedCurrency] = useState<string | null>(readCurrency)
  const [refreshing, setRefreshing] = useState(false)
  const refreshInFlight = useRef(false)
  const [refreshResults, setRefreshResults] = useState<MasterRefreshOutcome[] | null>(null)
  const [refreshError, setRefreshError] = useState<string | null>(null)

  const loadStatus = useCallback(async () => {
    setStatusLoading(true)
    setCurrencyLoading(true)
    setStatusError(null)
    setCurrencyError(null)

    // Independent requests: one failed endpoint must not hide the other's data.
    await Promise.all([
      fetchStatus()
        .then(setStatuses)
        .catch((error: unknown) => {
          setStatuses([])
          setStatusError(`Couldn't load shared masters from Integration: ${errorMessage(error)}`)
        })
        .finally(() => setStatusLoading(false)),
      fetchCurrencies()
        .then(codes => setCurrencies([...new Set(codes)].sort()))
        .catch((error: unknown) => {
          setCurrencies([])
          setCurrencyError(`Couldn't load currencies from Integration: ${errorMessage(error)}`)
        })
        .finally(() => setCurrencyLoading(false)),
    ])
  }, [])

  useEffect(() => {
    void loadStatus()
  }, [loadStatus])

  const refreshAll = async () => {
    if (refreshInFlight.current) return
    refreshInFlight.current = true
    setRefreshing(true)
    setRefreshResults(null)
    setRefreshError(null)
    try {
      setRefreshResults(await triggerRefresh())
    } catch (error) {
      setRefreshError(`Integration master refresh failed: ${errorMessage(error)}`)
    } finally {
      await loadStatus()
      refreshInFlight.current = false
      setRefreshing(false)
    }
  }

  const pickCurrency = (isoCode: string) => {
    setSelectedCurrency(isoCode)
    saveCurrency(isoCode)
  }

  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">Settings</h1>
      <p className="text-[1.1rem] text-muted-700">Manage your display currency and the shared master data for Travel and Property.</p>

      <div className="mt-8">
        <h2 className="mb-3 font-display text-[1.3rem] font-semibold">Display currency</h2>
        {currencyLoading ? (
          <p role="status" className="text-muted-600">Loading currencies...</p>
        ) : currencyError ? (
          <p role="alert" className="text-error">{currencyError}</p>
        ) : currencies.length === 0 ? (
          <p className="text-muted-600">No currencies loaded yet - run a master data refresh below to pull them in.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {currencies.map(code => (
              <button
                key={code}
                type="button"
                aria-pressed={selectedCurrency === code}
                onClick={() => pickCurrency(code)}
                className={`rounded-full border-2 px-4 py-1.5 text-sm font-semibold ${
                  selectedCurrency === code ? 'border-brand-900 bg-brand-100 text-brand-900' : 'border-line bg-white text-muted-700 hover:border-brand-700'
                }`}
              >
                {code}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="mt-10 flex items-center justify-between">
        <h2 className="font-display text-[1.3rem] font-semibold">Master data</h2>
        <button
          type="button"
          onClick={refreshAll}
          disabled={refreshing}
          className="flex items-center gap-2 rounded-full bg-brand-900 px-5 py-2.5 text-sm font-bold text-white disabled:opacity-60"
        >
          <ArrowPathIcon className={`size-4 ${refreshing ? 'animate-spin' : ''}`} aria-hidden="true" />
          {refreshing ? 'Refreshing...' : 'Refresh master data'}
        </button>
      </div>
      <p className="mt-2 text-[.9rem] text-muted-600">
        Integration refreshes the shared city and currency masters once for both services. Provider failures and partial results appear below.
      </p>

      {(refreshResults !== null || refreshError) && (
        <section aria-labelledby="refresh-results-heading" aria-live="polite" className="mt-6 rounded-xl border border-line bg-white px-4 py-3 text-sm">
          <h3 id="refresh-results-heading" className="mb-2 font-bold">Refresh results</h3>
          {refreshError && <p role="alert" className="text-error">{refreshError}</p>}
          {refreshResults?.length === 0 && <p>Integration returned no refresh outcomes.</p>}
          {refreshResults?.map(outcome => (
            <p key={outcome.masterName} className={outcome.status === 'SUCCESS' ? 'text-muted-700' : 'text-error'}>
              {outcome.masterName}: {outcome.status.toLowerCase()} ({outcome.recordsUpserted} saved)
              {outcome.errorMessage ? ` - ${outcome.errorMessage}` : ''}
            </p>
          ))}
        </section>
      )}

      <section aria-labelledby="shared-masters-heading" className="mt-6">
        <h3 id="shared-masters-heading" className="mb-2 text-[.95rem] font-bold text-muted-700 uppercase tracking-wide">Shared masters</h3>
        {statusLoading ? (
          <p role="status" className="text-muted-600">Loading shared masters...</p>
        ) : statusError ? (
          <p role="alert" className="rounded-xl border border-error/30 bg-white px-4 py-3 text-error">{statusError}</p>
        ) : statuses.length === 0 ? (
          <p className="text-muted-600">No shared masters reported by Integration yet.</p>
        ) : (
          <ul aria-label="Shared masters" className="flex flex-col gap-2">
            {statuses.map(status => (
              <DataRow
                key={status.masterName}
                leading={status.masterName}
                title={`${status.recordCount} record${status.recordCount === 1 ? '' : 's'}`}
                detail={`Last refreshed: ${formatTime(status.lastRefreshedAt)}`}
                trailing={status.lastStatus ?? '-'}
              />
            ))}
          </ul>
        )}
      </section>
    </SectionPanel>
  )
}