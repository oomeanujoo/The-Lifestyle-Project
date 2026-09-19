import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { SectionPanel } from '../../components/SectionPanel'
import { destinationPhoto, recommendRoad, type RoadJourney } from './routePlanningApi'

export function RouteDetailPage() {
  const [params] = useSearchParams()
  const origin = params.get('origin')?.trim() ?? ''
  const destination = params.get('destination')?.trim() ?? ''
  const vias = params.get('via')?.split('|').filter(Boolean) ?? []
  const viaKey = vias.join('|')
  const [route, setRoute] = useState<RoadJourney | null>(null)
  const [coverPhoto, setCoverPhoto] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!origin || !destination) return
    let active = true
    recommendRoad(origin, destination, viaKey ? viaKey.split('|') : [])
      .then(result => { if (active) setRoute(result) })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Could not load this route.')
      })
    return () => { active = false }
  }, [origin, destination, viaKey])

  useEffect(() => {
    if (!route) return
    let active = true
    destinationPhoto(route.destination.label).then(photo => { if (active) setCoverPhoto(photo) })
    return () => { active = false }
  }, [route])

  return (
    <SectionPanel>
      <Link to="/travel" className="text-sm font-semibold text-brand-800">← Back to Travel</Link>
      <h1 className="my-5 font-display text-4xl font-semibold">Route details</h1>
      {(!origin || !destination) && <p role="alert">Choose From and To on the Travel page first.</p>}
      {error && <p role="alert">{error}</p>}
      {origin && destination && !route && !error && <p>Loading route…</p>}
      {route && (
        <article className="overflow-hidden rounded-2xl border-2 border-line bg-white">
          <div className="relative flex min-h-[260px] items-center justify-center bg-gradient-to-br from-brand-900 via-brand-700 to-brand-100 p-6 text-center text-white">
            {coverPhoto && <img src={coverPhoto} alt="" className="absolute inset-0 h-full w-full object-cover opacity-45" />}
            <div className="absolute inset-0 bg-gradient-to-b from-brand-900/35 to-brand-900/55" />
            <div className="relative z-10">
              <p className="text-xs font-bold uppercase tracking-widest">Route details</p>
              <h2 className="mt-2 font-display text-3xl font-semibold">{route.origin.label} → {route.destination.label}</h2>
            {(route.vias ?? []).length > 0 && <p className="mt-1 text-lg">Via {route.vias.map(point => point.label).join(' → ')}</p>}
            </div>
          </div>
          <div className="p-6">
          {coverPhoto && <p className="mb-3 text-xs text-muted-600">Destination photo: Wikimedia Commons / Wikipedia.</p>}
          <h3 className="font-display text-xl font-semibold">By road</h3>
          {route.roadRoute ? (
            <>
              <p className="mt-4">Road distance: {route.roadRoute.distanceKm.toFixed(1)} km</p>
              <p>Estimated driving time: {Math.round(route.roadRoute.durationMinutes)} minutes</p>
              <p>Source: {route.roadRoute.source}; captured {new Date(route.roadRoute.capturedAt).toLocaleString()}</p>
              <p className="text-sm text-muted-600">Map data © <a href="https://www.openstreetmap.org/copyright" className="underline">OpenStreetMap contributors</a>.</p>
              {route.fuelCostEstimate && (
                <p className="mt-3">
                  Estimated fuel cost: {route.fuelCostEstimate.amount.toFixed(2)} {route.fuelCostEstimate.currency}
                  <span className="block text-sm text-muted-600">{route.fuelCostEstimate.label}</span>
                </p>
              )}
            </>
          ) : (
            <>
              <p className="mt-4">Approximate straight-line distance: {route.straightLineDistanceKm.toFixed(1)} km</p>
              <p className="text-sm text-muted-600">{route.straightLineLabel}</p>
              <p className="mt-2 text-sm text-muted-600">Road route unavailable: {route.roadRouteUnavailableReason}</p>
            </>
          )}
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl border border-line p-4"><h3 className="font-semibold">By train</h3><p className="mt-1 text-sm text-muted-600">No verified train schedules or fares available yet.</p></div>
            <div className="rounded-xl border border-line p-4"><h3 className="font-semibold">By air</h3><p className="mt-1 text-sm text-muted-600">No verified flight schedules or fares available yet.</p></div>
          </div>
          </div>
        </article>
      )}
    </SectionPanel>
  )
}
