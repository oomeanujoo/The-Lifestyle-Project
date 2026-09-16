import type { Trip } from '../tripData'
import type { Fact } from '../../../components/FactsBoard'
import { autoPick, formatDuration, formatFare, totalsFor } from './legFormat'

// Every fact here is *computed* from the app's own (illustrative, manually
// entered) trip data — never invented. No live/real-world claims (weather,
// real fare history) belong here unless backed by a real data source; see
// the decision log entry on why a "red alert" style fact was deliberately
// left out. Emoji live inline in the sentence, not pinned to the front.
export function buildFacts(trips: Trip[]): Fact[] {
  const facts: Fact[] = []

  for (const trip of trips) {
    if (trip.routes.length < 2) continue
    const totals = trip.routes.map(route => ({ route, ...totalsFor(route, autoPick(route, 'fare')) }))
    const byFare = [...totals].sort((a, b) => a.fareINR - b.fareINR)
    const byTime = [...totals].sort((a, b) => a.hours - b.hours)
    const [cheapest, priciest] = [byFare[0], byFare[byFare.length - 1]]
    if (cheapest.fareINR < priciest.fareINR) {
      facts.push({
        text: `Going ${trip.title.split(' → ').slice(0, 2).join(' → ')}? 😲 ${cheapest.route.label} beats ${priciest.route.label} by ${formatFare(priciest.fareINR - cheapest.fareINR)}.`,
      })
    }
    facts.push({
      text: `Fastest way there right now is ${byTime[0].route.label} ⚡ — just ${formatDuration(byTime[0].hours)}.`,
    })
  }

  if (trips.length > 1) {
    const grand = trips.map(trip => ({ trip, ...totalsFor(trip.routes[0], autoPick(trip.routes[0], 'fare')) }))
    const cheapest = [...grand].sort((a, b) => a.fareINR - b.fareINR)[0]
    const priciest = [...grand].sort((a, b) => b.fareINR - a.fareINR)[0]
    if (cheapest.trip.id !== priciest.trip.id) {
      facts.push({
        text: `${cheapest.trip.title} runs ${formatFare(priciest.fareINR - cheapest.fareINR)} 💸 cheaper than ${priciest.trip.title} — same wanderlust, smaller bill.`,
      })
    }
  }

  const routeCount = trips.reduce((sum, t) => sum + t.routes.length, 0)
  facts.push({
    text: `🗺️ ${trips.length} trip${trips.length === 1 ? '' : 's'} mapped, ${routeCount} route option${routeCount === 1 ? '' : 's'} to pick from.`,
  })

  return facts
}
