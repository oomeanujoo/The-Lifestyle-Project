import { useMemo, useState } from 'react'
import { FolderIcon } from '@heroicons/react/24/outline'
import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { FolderCard } from '../../components/FolderCard'
import { FactsBoard } from '../../components/FactsBoard'
import { PlaceAutocomplete } from '../../components/PlaceAutocomplete'
import type { PlaceMatch } from '../../lib/placeSearch'
import { trips } from './tripData'
import { buildFacts } from './components/factGenerators'

export function TravelFolderPage() {
  const [placeFilter, setPlaceFilter] = useState<PlaceMatch | null>(null)

  // Trips are filtered client-side against the place picked from the
  // DB-first search box — matching any leg's from/to, across every via
  // option, not just the primary route. See §18 "Smart search / autocomplete".
  const visibleTrips = useMemo(() => {
    if (!placeFilter) return trips
    return trips.filter(trip =>
      trip.routes.some(route => route.legs.some(leg => leg.from === placeFilter.label || leg.to === placeFilter.label)),
    )
  }, [placeFilter])

  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">Travel</h1>
      <p className="text-[1.1rem] text-muted-700">Two example trips, written up for the Phase 1 UI concept — not real bookings.</p>

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <PlaceAutocomplete scope="travel" placeholder="Search a place across your trips…" onSelect={setPlaceFilter} />
        {placeFilter && (
          <button
            type="button"
            onClick={() => setPlaceFilter(null)}
            className="rounded-full border-2 border-line px-3 py-1 text-[.78rem] font-semibold text-muted-700 hover:border-brand-700"
          >
            {placeFilter.label} ✕
          </button>
        )}
      </div>

      <div className="mt-6 grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-4.5">
        {visibleTrips.map(trip => (
          <FolderCard key={trip.id} Icon={FolderIcon} label={trip.title} image={trip.coverImage} to={`/travel/trips/${trip.id}`} />
        ))}
      </div>

      {visibleTrips.length === 0 && (
        <p className="mt-6 text-muted-700">No saved trips touch "{placeFilter?.label}" yet.</p>
      )}

      <FactsBoard facts={buildFacts(visibleTrips)} note="Computed from this app's own illustrative trip data — not live prices or real-world alerts." />
    </SectionPanel>
  )
}
