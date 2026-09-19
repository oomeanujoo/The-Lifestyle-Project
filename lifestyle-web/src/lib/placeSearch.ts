import { cities } from '../features/property/cityData'
import { findAreas } from '../features/property/areaData'
import { apiGet } from './apiClient'

export type PlaceKind = 'city' | 'area' | 'locality' | 'travel-place'

export type PlaceMatch = {
  id: string
  label: string
  sublabel?: string
  kind: PlaceKind
  to?: string
}

const MIN_QUERY_LENGTH = 2

const matches = (name: string, query: string) => name.toLowerCase().includes(query.toLowerCase())

function searchProperty(query: string): PlaceMatch[] {
  // STILL STATIC, deliberately, for now — not an oversight. Property's
  // city detail pages (CityDetailPage etc.) look up a city by the static
  // fixture id ("pune", "bengaluru"), not a live lifestyle_master.city
  // UUID. Swapping this search to the live backend (which returns real
  // UUIDs) would produce results that 404 when clicked, since nothing on
  // the Property side reads a live city yet. Fixing this for real means
  // making Property's browsing pages live too — the same shape of gap as
  // Travel's "save a route as a trip" problem — not something to bolt on
  // by itself. See TECHNICAL_ARCHITECTURE_DECISION_LOG.md, 2026-09-19.
  const results: PlaceMatch[] = []

  for (const city of cities) {
    if (matches(city.name, query)) {
      results.push({ id: `city-${city.id}`, label: city.name, sublabel: 'City', kind: 'city', to: `/property/cities/${city.id}` })
    }

    for (const area of findAreas(city.id)) {
      if (matches(area.name, query)) {
        results.push({
          id: `area-${city.id}-${area.id}`,
          label: area.name,
          sublabel: `Area · ${city.name}`,
          kind: 'area',
          to: `/property/cities/${city.id}/areas/${area.id}`,
        })
      }

      for (const locality of area.localities) {
        if (matches(locality.name, query)) {
          results.push({
            id: `locality-${city.id}-${area.id}-${locality.id}`,
            // Same locality name can exist under different areas/cities — parent
            // context in the label disambiguates them (§18 "Gandhi Road, Pune" vs
            // "Gandhi Road, Mumbai" pattern), never an AI guess at which one you meant.
            label: `${locality.name}, ${area.name}`,
            sublabel: `Locality · ${city.name} · ${locality.pincode}`,
            kind: 'locality',
            to: `/property/cities/${city.id}/areas/${area.id}/localities/${locality.id}`,
          })
        }
      }
    }
  }

  return results
}

type BackendPlaceMatch = { id: string; label: string; sublabel: string | null; kind: string }
type BackendPlaceSearchResponse = { matches: BackendPlaceMatch[]; aiFallbackNotice: string | null }

// Real, live search against travel-service's /places/search — which itself
// now queries the refreshed lifestyle_master.city masters (§13 decision
// log, 2026-09-19), never a bundled list. Travel has no per-place detail
// page to navigate to (unlike Property), so a live city name is a safe,
// complete result here — nothing downstream can 404 on it.
async function searchTravel(query: string): Promise<PlaceMatch[]> {
  const response = await apiGet<BackendPlaceSearchResponse>(`/api/travel/v1/places/search?q=${encodeURIComponent(query)}`)
  return response.matches.map(match => ({
    id: match.id,
    label: match.label,
    sublabel: match.sublabel ?? undefined,
    kind: match.kind.toLowerCase().replace(/_/g, '-') as PlaceKind,
  }))
}

// DB-first place search. Travel is genuinely live now; Property remains
// the static illustrative data described above until its browsing pages
// are made live too. See TECHNICAL_ARCHITECTURE.md §18 "Smart search /
// autocomplete".
export async function searchPlaces(scope: 'travel' | 'property', query: string): Promise<PlaceMatch[]> {
  const trimmed = query.trim()
  if (trimmed.length < MIN_QUERY_LENGTH) return []
  return scope === 'travel' ? searchTravel(trimmed) : Promise.resolve(searchProperty(trimmed))
}

// Honest stand-in for the AI-fallback step designed in §18: no AI call
// happens here — when nothing local matches, the UI surfaces this exact
// state instead of hiding it or pretending to call AI.
export const AI_FALLBACK_NOTICE =
  'No local match. Once an AI provider is wired up here (see TECHNICAL_ARCHITECTURE.md §18), an AI-drafted suggestion would appear instead — clearly marked unverified, never silently trusted.'
