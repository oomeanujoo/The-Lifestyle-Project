import { cities } from '../features/property/cityData'
import { findAreas } from '../features/property/areaData'
import { trips } from '../features/travel/tripData'

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

function searchTravel(query: string): PlaceMatch[] {
  const placeToTrips = new Map<string, Set<string>>()

  for (const trip of trips) {
    for (const route of trip.routes) {
      for (const leg of route.legs) {
        for (const place of [leg.from, leg.to]) {
          if (!matches(place, query)) continue
          if (!placeToTrips.has(place)) placeToTrips.set(place, new Set())
          placeToTrips.get(place)?.add(trip.title)
        }
      }
    }
  }

  return [...placeToTrips.entries()].map(([place, tripTitles]) => ({
    id: `travel-place-${place}`,
    label: place,
    sublabel: `Appears in: ${[...tripTitles].join(', ')}`,
    kind: 'travel-place' as const,
  }))
}

// DB-first place search — the local `city`/`area`/`locality` data (and, for
// Travel, the place names already saved inside this app's own trip legs) is
// checked first and returned instantly, with zero AI calls. See
// TECHNICAL_ARCHITECTURE.md §18 "Smart search / autocomplete". A real backend
// would query Postgres masters here instead of these static arrays — the
// search *shape* (local-first, disambiguated by parent context) doesn't
// change when that swap happens later, only where the data comes from.
export function searchPlaces(scope: 'travel' | 'property', query: string): PlaceMatch[] {
  const trimmed = query.trim()
  if (trimmed.length < MIN_QUERY_LENGTH) return []
  return scope === 'travel' ? searchTravel(trimmed) : searchProperty(trimmed)
}

// Honest stand-in for the AI-fallback step designed in §18: this app has no
// backend and no AI provider wired up yet, so rather than fake a call, the UI
// surfaces this exact state instead of hiding it or pretending to call AI.
export const AI_FALLBACK_NOTICE =
  'No local match. Once a backend and AI provider are wired up (see TECHNICAL_ARCHITECTURE.md §18), an AI-drafted suggestion would appear here instead — clearly marked unverified, never silently trusted.'
