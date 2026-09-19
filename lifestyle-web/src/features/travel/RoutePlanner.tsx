import { useEffect, useId, useState } from 'react'
import { Link } from 'react-router'
import { destinationPhoto, recommendRoad, suggestCities, type RoadJourney } from './routePlanningApi'

const message = (error: unknown) => (error instanceof Error ? error.message : 'Unexpected request failure')

type Stop = 'From' | 'To' | 'Via'

const formatKm = (km: number) => `${km.toLocaleString(undefined, { maximumFractionDigits: 1 })} km`
const formatMinutes = (minutes: number) => {
  const hours = Math.floor(minutes / 60)
  const remaining = Math.round(minutes % 60)
  return hours > 0 ? `${hours}h ${remaining}m` : `${remaining}m`
}

export function RoutePlanner() {
  const headingId = useId()
  const inputId = useId()
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [vias, setVias] = useState<string[]>([])
  const [draft, setDraft] = useState('')
  const [editing, setEditing] = useState<Stop | number | null>(null)
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [focused, setFocused] = useState(false)
  const [result, setResult] = useState<RoadJourney | null>(null)
  const [coverPhoto, setCoverPhoto] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [searching, setSearching] = useState(false)
  const nextStop: Stop = typeof editing === 'number' ? 'Via' : editing ?? (!origin ? 'From' : !destination ? 'To' : 'Via')

  useEffect(() => {
    if (!result) { setCoverPhoto(null); return }
    let active = true
    destinationPhoto(result.destination.label).then(photo => { if (active) setCoverPhoto(photo) })
    return () => { active = false }
  }, [result])

  useEffect(() => {
    const query = draft.trim()
    if (query.length < 2) {
      setSuggestions([])
      return
    }
    let active = true
    const timer = setTimeout(() => {
      suggestCities(query)
        .then(matches => { if (active) setSuggestions(matches) })
        .catch(() => { if (active) setSuggestions([]) })
    }, 200)
    return () => { active = false; clearTimeout(timer) }
  }, [draft])

  const commit = (city: string) => {
    const name = city.trim()
    if (!name) return
    if (nextStop === 'From') setOrigin(name)
    else if (nextStop === 'To') setDestination(name)
    else if (typeof editing === 'number') setVias(current => current.map((value, index) => index === editing ? name : value))
    else setVias(current => [...current, name])
    setDraft('')
    setEditing(null)
    setSuggestions([])
    setError(null)
    setResult(null)
  }

  const edit = (stop: Stop | number) => {
    const current = stop === 'From' ? origin : stop === 'To' ? destination : typeof stop === 'number' ? vias[stop] : ''
    setDraft(current)
    setEditing(stop)
    if (stop === 'From') setOrigin('')
    else if (stop === 'To') setDestination('')
    // Keep the existing slot until the replacement is committed, so order stays stable.
    setResult(null)
    document.getElementById(inputId)?.focus()
  }

  const search = async () => {
    const from = origin || (nextStop === 'From' ? draft.trim() : '')
    const to = destination || (nextStop === 'To' ? draft.trim() : '')
    const routeVias = typeof editing === 'number' && draft.trim()
      ? vias.map((city, index) => index === editing ? draft.trim() : city)
      : [...vias, ...(nextStop === 'Via' && draft.trim() ? [draft.trim()] : [])]
    if (!from || !to) {
      setError('Add a From city and a To city first.')
      return
    }
    setOrigin(from)
    setDestination(to)
    setVias(routeVias)
    setDraft('')
    setEditing(null)
    setSearching(true)
    setError(null)
    setResult(null)
    try {
      setResult(await recommendRoad(from, to, routeVias))
    } catch (err) {
      setError(message(err))
    } finally {
      setSearching(false)
    }
  }

  return (
    <section aria-labelledby={headingId} className="my-8">
      <h2 id={headingId} className="font-display text-2xl font-semibold">Where do you want to go?</h2>
      <p className="mt-2 text-sm text-muted-600">Type From and To, then add any number of optional Via stops. Suggestions come from refreshed city data.</p>

      <form noValidate onSubmit={event => { event.preventDefault(); void search() }} className="mt-6">
        <div className="relative rounded-2xl border-2 border-line bg-white px-4 py-4 shadow-[0_12px_36px_#233b2d12] focus-within:border-brand-700">
          <div className="flex min-h-16 flex-wrap items-center gap-2">
            {([['From', origin], ['To', destination]] as const).map(([stop, city]) => city && (
              <button key={stop} type="button" onClick={() => edit(stop)} aria-label={`Edit ${stop}: ${city}`}
                className="inline-flex items-center gap-1.5 rounded-xl bg-brand-100 px-3 py-2 text-sm text-brand-900 hover:bg-brand-200">
                <span className="font-semibold">{stop}</span><span>{city}</span><span aria-hidden="true">×</span>
              </button>
            ))}
            {vias.map((city, index) => editing === index ? null : (
              <button key={`${city}-${index}`} type="button" onClick={() => edit(index)} aria-label={`Edit Via ${index + 1}: ${city}`}
                className="inline-flex items-center gap-1.5 rounded-xl bg-brand-100 px-3 py-2 text-sm text-brand-900 hover:bg-brand-200">
                <span className="font-semibold">Via {index + 1}</span><span>{city}</span><span aria-hidden="true">×</span>
              </button>
            ))}
            <label htmlFor={inputId} className="sr-only">{`Add ${nextStop} city`}</label>
            <input id={inputId} type="text" autoComplete="off" value={draft}
              placeholder={nextStop === 'Via' ? 'Add a via city (optional)' : `Type ${nextStop.toLowerCase()} city…`}
              onChange={event => { setDraft(event.target.value); setResult(null); setError(null) }}
              onFocus={() => setFocused(true)} onBlur={() => setTimeout(() => setFocused(false), 120)}
              onKeyDown={event => {
                if ((event.key === 'Enter' || event.key === 'Tab') && draft.trim()) {
                  event.preventDefault()
                  commit(draft)
                }
              }}
              className="min-w-48 flex-1 bg-transparent px-2 py-2 text-lg text-ink outline-none" />
          </div>
          {focused && suggestions.length > 0 && (
            <ul aria-label={`${nextStop} city suggestions`} className="absolute left-4 right-4 top-full z-20 mt-2 max-h-56 overflow-auto rounded-xl border border-line bg-white py-1 shadow-lg">
              {suggestions.map(city => <li key={city}><button type="button" onMouseDown={event => event.preventDefault()} onClick={() => commit(city)}
                className="block w-full px-4 py-2 text-left hover:bg-brand-100">{city}</button></li>)}
            </ul>
          )}
        </div>
        <div className="mt-4 flex flex-wrap items-center gap-4">
          <button type="submit" disabled={searching} className="rounded-full bg-brand-900 px-7 py-3 font-bold text-white disabled:opacity-60">
            {searching ? 'Finding route...' : 'Find route'}
          </button>
          <span className="text-sm text-muted-600">Select a suggestion or press Enter to add each city. Via stops follow the order shown.</span>
        </div>
      </form>

      <div aria-live="polite" className="mt-7 flex flex-col">
        {error && <p role="alert" className="text-error">{error}</p>}
        {result && (
          <Link to={`/travel/routes/detail?${new URLSearchParams({ origin: result.origin.label, destination: result.destination.label, ...((result.vias ?? []).length ? { via: result.vias.map(point => point.label).join('|') } : {}) })}`}
            className="group order-2 mt-5 block max-w-[380px] overflow-hidden rounded-2xl bg-white shadow-[0_8px_26px_#233b2d18] transition hover:-translate-y-0.5 hover:shadow-[0_16px_40px_#233b2d26]"
            aria-label={`Open route details for ${result.origin.label} to ${result.destination.label}`}>
            <div className="relative flex min-h-[220px] items-end bg-gradient-to-br from-brand-900 via-brand-700 to-brand-100 p-6 text-white">
              {coverPhoto && <img src={coverPhoto} alt="" className="absolute inset-0 h-full w-full object-cover opacity-50" />}
              <div className="absolute inset-0 bg-gradient-to-b from-transparent to-brand-900/85" />
              <div className="relative z-10">
                <p className="text-xs font-bold uppercase tracking-widest">Your route</p>
                <h3 className="mt-2 font-display text-2xl font-semibold">{result.origin.label} → {result.destination.label}</h3>
                {(result.vias ?? []).length > 0 && <p className="mt-1 text-lg">Via {result.vias.map(point => point.label).join(' → ')}</p>}
              </div>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-4 p-5">
              <div>
                {result.roadRoute ? <>
                  <p className="font-semibold">Road route · {formatKm(result.roadRoute.distanceKm)} · about {formatMinutes(result.roadRoute.durationMinutes)}</p>
                  <p className="mt-1 text-sm text-muted-600">Source: {result.roadRoute.source}. Driving estimate; traffic, tolls and ticket prices unavailable.</p>
                  <p className="mt-1 text-xs text-muted-600">Map data © OpenStreetMap contributors.</p>
                  {result.fuelCostEstimate && <p className="mt-1 text-sm text-muted-600">Fuel estimate: {result.fuelCostEstimate.amount.toLocaleString(undefined, { maximumFractionDigits: 2 })} {result.fuelCostEstimate.currency} · {result.fuelCostEstimate.label}</p>}
                </> : <>
                  <p className="font-semibold">Straight-line comparison · {formatKm(result.straightLineDistanceKm)}</p>
                  <p className="mt-1 text-sm text-muted-600">{result.straightLineLabel}</p>
                  {result.roadRouteUnavailableReason && <p className="mt-1 text-sm text-muted-600">Road route unavailable: {result.roadRouteUnavailableReason}</p>}
                </>}
              </div>
              <span className="rounded-full bg-brand-900 px-4 py-2 text-sm font-semibold text-white group-hover:bg-brand-700">Open details →</span>
            </div>
            {coverPhoto && <p className="px-5 pb-3 text-xs text-muted-600">Destination photo: Wikimedia Commons / Wikipedia.</p>}
          </Link>
        )}
        {result && (result.suggestedViaCandidates ?? []).length > 0 && (
          <div className="order-1 mt-5 rounded-2xl border border-line bg-white p-5">
            <h3 className="font-display text-xl font-semibold">Suggested via cities</h3>
            <p className="mt-1 text-sm text-muted-600">More cities from the shared master, ordered by shortest estimated straight-line detour. Select one to add it to your route.</p>
            <div className="mt-3 flex flex-wrap gap-2">
              {result.suggestedViaCandidates.map(candidate => <button key={candidate.via.id ?? candidate.via.label} type="button"
                onClick={() => { setVias(current => [...current, candidate.via.label]); setResult(null) }}
                className="rounded-full border border-line bg-brand-100 px-4 py-2 text-sm font-semibold hover:border-brand-700">
                Via {candidate.via.label} · +{formatKm(candidate.detourKm)}
              </button>)}
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
