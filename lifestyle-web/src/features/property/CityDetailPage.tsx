import { useEffect } from 'react'
import { MapPinIcon } from '@heroicons/react/24/outline'
import { Navigate, useParams } from 'react-router'
import { Cover } from '../../components/Cover'
import { SectionPanel } from '../../components/SectionPanel'
import { RentSortList } from '../../components/RentSortList'
import { FactsBoard } from '../../components/FactsBoard'
import { recordAccess } from '../../lib/lastAccessed'
import { findCity } from './cityData'
import { areaAvgRent, findAreas } from './areaData'
import { buildCityAreaFacts } from './components/cityAreaFacts'

const money = (amount: number) => `₹${amount.toLocaleString('en-IN')}/mo avg`

// Areas show directly on the city page now — no separate "explore areas"
// click-through, and no cost-of-living/nearby-places boxes below it,
// per explicit direction to remove both.
export function CityDetailPage() {
  const { cityId } = useParams()
  const city = findCity(cityId)
  const areas = findAreas(cityId)

  useEffect(() => {
    if (city) recordAccess('property', city.name)
  }, [city])

  if (!city) return <Navigate to="/property" replace />

  const items = areas.map(area => ({
    id: area.id,
    label: area.name,
    sublabel: `${area.municipality} · ${money(areaAvgRent(area))}`,
    avgRentINR: areaAvgRent(area),
    to: `/property/cities/${city.id}/areas/${area.id}`,
  }))

  return (
    <SectionPanel>
      <Cover image={city.coverImage} title={city.name} />

      {areas.length > 0 ? (
        <>
          <h2 className="mt-8 mb-3 font-display text-[1.3rem] font-semibold">Areas in {city.name}</h2>
          <RentSortList items={items} Icon={MapPinIcon} />
          <FactsBoard facts={buildCityAreaFacts(city.name, city.id, areas)} note={`About ${city.name} — computed from this app's own area data, plus general knowledge, not live news.`} />
        </>
      ) : (
        <p className="mt-8 text-muted-700">Area-level breakdown for {city.name} isn't mapped yet.</p>
      )}
    </SectionPanel>
  )
}
