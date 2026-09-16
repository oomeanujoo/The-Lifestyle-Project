import { useEffect, useState } from 'react'
import type { RouteOption } from '../tripData'
import { LegPicker } from './LegPicker'
import { FareSummary } from './FareSummary'
import { RouteCompareStrip } from './RouteCompareStrip'
import { TripDates } from './TripDates'
import { JourneyMap } from './JourneyMap'
import { autoPick, totalsFor, type Selection, type SortCriterion } from './legFormat'

export function RouteComparison({ routes }: { routes: RouteOption[] }) {
  const [sortBy, setSortBy] = useState<SortCriterion>('fare')
  const [activeRouteId, setActiveRouteId] = useState(routes[0].id)
  const route = routes.find(r => r.id === activeRouteId) ?? routes[0]

  // Sorting by fare/duration re-picks the cheapest/fastest option on every
  // leg automatically — the user can still override a single leg below,
  // but switching route or sort criterion resets to the auto pick.
  const [selection, setSelection] = useState<Selection>(() => autoPick(route, sortBy))
  useEffect(() => setSelection(autoPick(route, sortBy)), [route, sortBy])

  const selectFor = (legIndex: number, option: Selection[number]) => setSelection(prev => ({ ...prev, [legIndex]: option }))
  const { fareINR: totalFareINR, hours: totalHours } = totalsFor(route, selection)

  return (
    <div>
      <div className="mb-4 flex items-center gap-2 text-[.88rem] text-muted-600">
        <span>Sort by</span>
        <button type="button" onClick={() => setSortBy('fare')} className={`rounded-full border px-3 py-1.25 text-[.82rem] ${sortBy === 'fare' ? 'border-brand-700 bg-brand-100 font-bold text-brand-900' : 'border-line bg-white'}`}>Cheapest fare</button>
        <button type="button" onClick={() => setSortBy('duration')} className={`rounded-full border px-3 py-1.25 text-[.82rem] ${sortBy === 'duration' ? 'border-brand-700 bg-brand-100 font-bold text-brand-900' : 'border-line bg-white'}`}>Shortest time</button>
      </div>

      <RouteCompareStrip routes={routes} sortBy={sortBy} activeRouteId={activeRouteId} onSelect={setActiveRouteId} />

      <TripDates oneWayFareINR={totalFareINR} />

      <div className="grid grid-cols-1 items-start gap-6 md:grid-cols-[1fr_220px]">
        <ol className="flex flex-col gap-2.5">
          {route.legs.map((leg, index) => (
            <LegPicker key={index} leg={leg} sortBy={sortBy} selected={selection[index] ?? leg.options[0]} onSelect={option => selectFor(index, option)} />
          ))}
        </ol>
        <FareSummary totalFareINR={totalFareINR} totalHours={totalHours} />
      </div>

      <JourneyMap route={route} selection={selection} />
    </div>
  )
}
