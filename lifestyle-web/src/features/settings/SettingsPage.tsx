import { useEffect, useState } from 'react'
import { ArrowPathIcon } from '@heroicons/react/24/outline'
import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { DataRow } from '../../components/DataRow'
import { readCurrency, saveCurrency } from '../../lib/currencyPreference'
import { SECTIONS, fetchStatus, triggerRefresh, fetchCurrencies, type MasterRefreshOutcome, type MasterStatus, type Section } from './masterRefreshApi'

type SectionState = {
  section: Section
  statuses: MasterStatus[] | null
  error: string | null
}

const formatTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : 'Never refreshed')

const connectionErrorMessage = (section: Section) =>
  `Couldn't reach ${section.label.toLowerCase()}-service. Is it running (see README "Start locally")?`

// Nothing on this page is hardcoded — every master name, count, timestamp
// and currency option comes back from the backend's own endpoints. If a
// service isn't running, that section shows an honest connection error
// instead of fabricated data. See TECHNICAL_ARCHITECTURE.md §20.
export function SettingsPage() {
  const [sections, setSections] = useState<SectionState[]>(
    SECTIONS.map(section => ({ section, statuses: null, error: null })),
  )
  const [currencies, setCurrencies] = useState<string[]>([])
  const [selectedCurrency, setSelectedCurrency] = useState<string | null>(readCurrency())
  const [refreshing, setRefreshing] = useState(false)
  const [refreshResults, setRefreshResults] = useState<{ section: Section; outcomes: MasterRefreshOutcome[]; error: string | null }[]>([])

  const loadStatus = async () => {
    const results = await Promise.all(
      SECTIONS.map(async section => {
        try {
          const statuses = await fetchStatus(section)
          return { section, statuses, error: null }
        } catch {
          return { section, statuses: null, error: connectionErrorMessage(section) }
        }
      }),
    )
    setSections(results)

    const currencyLists = await Promise.all(
      SECTIONS.map(section => fetchCurrencies(section).catch(() => [] as string[])),
    )
    setCurrencies([...new Set(currencyLists.flat())].sort())
  }

  useEffect(() => {
    loadStatus()
  }, [])

  const refreshAll = async () => {
    setRefreshing(true)
    try {
      const results = await Promise.all(
        SECTIONS.map(async section => {
          try {
            return { section, outcomes: await triggerRefresh(section), error: null }
          } catch (error) {
            return { section, outcomes: [] as MasterRefreshOutcome[], error: error instanceof Error ? error.message : 'Refresh failed' }
          }
        }),
      )
      setRefreshResults(results)
    } finally {
      await loadStatus()
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
      <p className="text-[1.1rem] text-muted-700">Just two things to manage — your display currency, and refreshing the master data every trip/property page reads from.</p>

      <div className="mt-8">
        <h2 className="mb-3 font-display text-[1.3rem] font-semibold">Display currency</h2>
        {currencies.length === 0 ? (
          <p className="text-muted-600">No currencies loaded yet — run a master data refresh below to pull them in.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {currencies.map(code => (
              <button
                key={code}
                type="button"
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
          {refreshing ? 'Refreshing…' : 'Refresh master data'}
        </button>
      </div>
      <p className="mt-2 text-[.9rem] text-muted-600">
        Pulls city data from GeoNames and exchange rates from Frankfurter, through integration-service, and upserts them — existing records are updated, never deleted (see §17).
      </p>

      {sections.map(({ section, statuses, error }) => (
        <div key={section.id} className="mt-6">
          <h3 className="mb-2 text-[.95rem] font-bold text-muted-700 uppercase tracking-wide">{section.label}</h3>
          {error && <p className="rounded-xl border border-error/30 bg-white px-4 py-3 text-error">{error}</p>}
          {refreshResults.filter(result => result.section.id === section.id).map(result => (
            <div key={`${section.id}-result`} className="mb-3 rounded-xl border border-line bg-white px-4 py-3 text-sm">
              {result.error && <p className="text-error">{result.error}</p>}
              {result.outcomes.map(outcome => (
                <p key={outcome.masterName} className={outcome.status === 'SUCCESS' ? 'text-muted-700' : 'text-error'}>
                  {outcome.masterName}: {outcome.status.toLowerCase()} ({outcome.recordsUpserted} saved)
                  {outcome.errorMessage ? ` — ${outcome.errorMessage}` : ''}
                </p>
              ))}
            </div>
          ))}
          {statuses && (
            <ul className="flex flex-col gap-2">
              {statuses.map(status => (
                <DataRow
                  key={status.masterName}
                  leading={status.masterName}
                  title={`${status.recordCount} record${status.recordCount === 1 ? '' : 's'}`}
                  detail={`Last refreshed: ${formatTime(status.lastRefreshedAt)}`}
                  trailing={status.lastStatus ?? '—'}
                />
              ))}
            </ul>
          )}
        </div>
      ))}
    </SectionPanel>
  )
}
