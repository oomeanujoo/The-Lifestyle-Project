import type { RouteOption } from '../tripData'
import { autoPick, formatDuration, formatFare, totalsFor, type SortCriterion } from './legFormat'

// The side-by-side "via Mumbai vs. via Delhi" comparison — each route's
// total (for the current sort criterion) shown at a glance, with the
// winner badged, before drilling into leg-by-leg detail below.
export function RouteCompareStrip({ routes, sortBy, activeRouteId, onSelect }: {
  routes: RouteOption[]
  sortBy: SortCriterion
  activeRouteId: string
  onSelect: (routeId: string) => void
}) {
  if (routes.length < 2) return null

  const totals = routes.map(route => ({ route, ...totalsFor(route, autoPick(route, sortBy)) }))
  const bestValue = Math.min(...totals.map(t => (sortBy === 'fare' ? t.fareINR : t.hours)))

  return (
    <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
      {totals.map(({ route, fareINR, hours }) => {
        const isBest = (sortBy === 'fare' ? fareINR : hours) === bestValue
        const isActive = route.id === activeRouteId
        return (
          <button
            key={route.id}
            type="button"
            onClick={() => onSelect(route.id)}
            className={`relative rounded-2xl border-2 p-4 text-left ${isActive ? 'border-brand-950 bg-brand-100' : 'border-line bg-white'}`}
          >
            {isBest && (
              <span className="absolute top-3 right-3 rounded-full bg-brand-700 px-2.5 py-1 text-[.7rem] font-bold tracking-wide text-white uppercase">
                {sortBy === 'fare' ? 'Cheapest' : 'Fastest'}
              </span>
            )}
            <p className="font-display text-lg font-semibold text-ink">{route.label}</p>
            <p className="mt-2 font-sans text-2xl font-bold tabular-nums text-brand-950">{formatFare(fareINR)}</p>
            <p className="text-[.9rem] text-muted-700">{formatDuration(hours)} total &middot; {route.legs.length} leg{route.legs.length > 1 ? 's' : ''}</p>
          </button>
        )
      })}
    </div>
  )
}
