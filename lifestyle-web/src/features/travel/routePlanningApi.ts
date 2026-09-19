// The real contract, backed by RouteRecommendationController
// (travel-service) — replaces the earlier PROVISIONAL placeholder that
// expected a dropdown-of-places API (`/route-planning/{places,routes}`).
// Origin/destination/via are typed city NAMES, resolved server-side
// against the live, GeoNames-refreshed lifestyle_master.city masters —
// never a hardcoded frontend list, never an id you pick from a preloaded
// dropdown.
export type RoutePoint = {
  id: string | null
  label: string
  countryCode: string
  latitude: number
  longitude: number
  source: string | null
  asOf: string | null
}

export type RouteCandidate = {
  via: RoutePoint
  originToViaKm: number
  viaToDestinationKm: number
  totalKm: number
  detourKm: number
}

export type RouteRecommendation = {
  origin: RoutePoint
  destination: RoutePoint
  directDistanceKm: number
  distanceLabel: string
  requestedVia: RouteCandidate | null
  suggestedViaCandidates: RouteCandidate[]
}

const BASE = '/api/travel/v1/routes'

// Wikimedia supplies a destination photo when it has a page thumbnail.
// A missing or ambiguous page leaves the card's styled background intact.
export async function destinationPhoto(city: string): Promise<string | null> {
  try {
    const response = await fetch(`https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(city)}?origin=*`)
    if (!response.ok) return null
    const page = (await response.json()) as { type?: string; thumbnail?: { source?: string } }
    return page.type === 'standard' && page.thumbnail?.source?.startsWith('https://') ? page.thumbnail.source : null
  } catch {
    return null
  }
}

// Live typeahead — every keystroke (debounced by the caller) asks the
// backend for real matches against lifestyle_master.city. Never a bundled
// city list on this side.
export async function suggestCities(query: string): Promise<string[]> {
  const trimmed = query.trim()
  if (trimmed.length < 2) return []
  const response = await fetch(`${BASE}/city-suggestions?q=${encodeURIComponent(trimmed)}`)
  if (!response.ok) throw new Error(`City suggestions failed with HTTP ${response.status}`)
  return response.json() as Promise<string[]>
}

export async function recommendRoute(origin: string, destination: string, via?: string): Promise<RouteRecommendation> {
  const params = new URLSearchParams({ origin, destination })
  if (via) params.set('via', via)
  const response = await fetch(`${BASE}/recommend?${params.toString()}`)
  const body = (await response.json()) as RouteRecommendation & { error?: string }
  if (!response.ok) throw new Error(body.error ?? `Route recommendation failed with HTTP ${response.status}`)
  return body
}

// A real road route (OSRM demo) alongside the same straight-line
// comparison recommendRoute() returns — backed by
// GET /api/travel/v1/routes/recommend-road. `roadRoute`/`fuelCostEstimate`
// are null together whenever a real route wasn't obtained (provider
// disabled, timed out, no route, rate limited); `roadRouteUnavailableReason`
// then explains why. `straightLineDistanceKm`/`straightLineLabel` are
// always present, so the caller never has nothing to show.
export type RoadRoute = {
  distanceKm: number
  durationMinutes: number
  geometry: string | null
  source: string
  capturedAt: string
}

export type FuelCostEstimate = {
  amount: number
  currency: string
  fuelPricePerLitre: number
  vehicleKmPerLitre: number
  label: string
}

export type RoadJourney = {
  origin: RoutePoint
  destination: RoutePoint
  via: RoutePoint | null
  vias: RoutePoint[]
  suggestedViaCandidates: RouteCandidate[]
  straightLineDistanceKm: number
  straightLineLabel: string
  roadRoute: RoadRoute | null
  roadRouteUnavailableReason: string | null
  fuelCostEstimate: FuelCostEstimate | null
}

export async function recommendRoad(origin: string, destination: string, via: string | string[] = []): Promise<RoadJourney> {
  const params = new URLSearchParams({ origin, destination })
  for (const city of (Array.isArray(via) ? via : [via])) if (city) params.append('via', city)
  const url = `${BASE}/recommend-road?${params.toString()}`
  const attempted = new Set<string>()
  for (;;) {
    const response = await fetch(url)
    const body = (await response.json()) as RoadJourney & { error?: string }
    if (response.ok) return body
    const error = body.error ?? `Road route request failed with HTTP ${response.status}`
    // Only an explicit Find route can reach this path. GeoNames verifies a
    // missing Indian city before it enters the shared master; AI drafts never do.
    const missing = response.status === 400 &&
      /^(?:origin|destination|via) '([^']+)' (?:was not found|did not match)/.exec(error)?.[1]
    if (!missing || attempted.has(missing) || attempted.size >= 3 || /^\d{6}$/.test(missing)) throw new Error(error)
    attempted.add(missing)
    const acquired = await fetch(`/api/integration/v1/masters/lifestyle/cities/acquire?${new URLSearchParams({ name: missing, countryCode: 'IN' })}`, { method: 'POST' })
    if (!acquired.ok) throw new Error(`${error}. Verified city lookup returned HTTP ${acquired.status}.`)
  }
}
