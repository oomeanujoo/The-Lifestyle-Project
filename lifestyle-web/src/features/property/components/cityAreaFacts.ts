import type { Fact } from '../../../components/FactsBoard'
import { areaAvgRent, type Area } from '../areaData'

const money = (amount: number) => `₹${amount.toLocaleString('en-IN')}/mo`

// A couple of stable, broadly-known facts about a city (not live news —
// see the note on the entry itself). Everything else on this list is
// computed from the app's own area data, same rule as every other facts
// list in this app.
const GENERAL_CITY_NOTES: Record<string, string[]> = {
  pune: [
    'Hinjewadi and Magarpatta are two of the best-known IT hub areas in Pune, which is part of why rents there track the tech job market.',
  ],
}

export function buildCityAreaFacts(cityName: string, cityId: string, areas: Area[]): Fact[] {
  const facts: Fact[] = []
  if (areas.length === 0) return facts

  const withRent = areas.map(area => ({ area, avg: areaAvgRent(area) }))
  const priciest = [...withRent].sort((a, b) => b.avg - a.avg)[0]
  const cheapest = [...withRent].sort((a, b) => a.avg - b.avg)[0]
  if (priciest.area.id !== cheapest.area.id) {
    facts.push({
      text: `${priciest.area.name} runs the highest average rent in ${cityName} right now, at ${money(priciest.avg)} — ${cheapest.area.name} is the most affordable of the areas listed, at ${money(cheapest.avg)}.`,
    })
  }

  const pmcCount = areas.filter(a => a.municipality === 'PMC').length
  const pcmcCount = areas.filter(a => a.municipality === 'PCMC').length
  if (pmcCount > 0 && pcmcCount > 0) {
    facts.push({
      text: `Of the areas listed for ${cityName}, ${pmcCount} fall under PMC and ${pcmcCount} under PCMC — worth checking which municipality an area belongs to before you commit.`,
    })
  }

  const localityCount = areas.reduce((sum, a) => sum + a.localities.length, 0)
  facts.push({
    text: `${areas.length} areas and ${localityCount} localities mapped for ${cityName} so far — this list grows as more get added.`,
  })

  for (const note of GENERAL_CITY_NOTES[cityId] ?? []) {
    facts.push({ text: note })
  }

  return facts
}
