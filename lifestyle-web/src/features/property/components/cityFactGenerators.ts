import type { City } from '../cityData'
import type { Fact } from '../../../components/FactsBoard'

const money = (amount: number) => `₹${amount.toLocaleString('en-IN')}`
const monthlyTotal = (city: City) => city.costOfLiving.reduce((sum, line) => sum + line.monthlyINR, 0)

// Every fact here is computed from the app's own illustrative city data —
// see the equivalent note in features/travel/components/factGenerators.ts.
// Emoji live inline in the sentence, not pinned to the front.
export function buildCityFacts(cities: City[]): Fact[] {
  const facts: Fact[] = []
  if (cities.length < 2) return facts

  const totals = cities.map(city => ({ city, total: monthlyTotal(city) }))
  const cheapest = [...totals].sort((a, b) => a.total - b.total)[0]
  const priciest = [...totals].sort((a, b) => b.total - a.total)[0]
  if (cheapest.city.id !== priciest.city.id) {
    facts.push({
      text: `${cheapest.city.name} runs ${money(priciest.total - cheapest.total)} 💰 cheaper per month to live in than ${priciest.city.name}.`,
    })
  }

  for (const city of cities) {
    const rent = city.costOfLiving.find(line => line.label.startsWith('Rent'))
    const total = monthlyTotal(city)
    if (rent) {
      facts.push({
        text: `🏠 Rent alone is ${Math.round((rent.monthlyINR / total) * 100)}% of the monthly cost of living in ${city.name}.`,
      })
    }
  }

  facts.push({
    text: `${cities.reduce((sum, c) => sum + c.nearby.length, 0)} nearby spots 🍽️ noted across ${cities.length} cities so far.`,
  })

  return facts
}
