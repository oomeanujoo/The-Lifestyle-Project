import { useEffect } from 'react'
import { Navigate, useParams } from 'react-router'
import { Cover } from '../../components/Cover'
import { SectionPanel } from '../../components/SectionPanel'
import { recordAccess } from '../../lib/lastAccessed'
import { findTrip } from './tripData'
import { RouteComparison } from './components/RouteComparison'
import { ItinerarySection } from './components/ItinerarySection'

export function TripDetailPage() {
  const { tripId } = useParams()
  const trip = findTrip(tripId)

  useEffect(() => {
    if (trip) recordAccess('travel', trip.title)
  }, [trip])

  if (!trip) return <Navigate to="/travel" replace />

  return (
    <SectionPanel>
      <Cover image={trip.coverImage} title={trip.title} />

      <h2 className="mt-8 mb-3 font-display text-[1.3rem] font-semibold">Route</h2>
      <RouteComparison routes={trip.routes} />

      <ItinerarySection days={trip.itinerary} />

      <span className="mt-6 block text-muted-500">
        Placeholder planning content — not a booking, not sourced. See docs/product-concept-ui.md.
      </span>
    </SectionPanel>
  )
}
