import { BuildingOffice2Icon } from '@heroicons/react/24/outline'
import { useNavigate } from 'react-router'
import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { FolderCard } from '../../components/FolderCard'
import { FactsBoard } from '../../components/FactsBoard'
import { PlaceAutocomplete } from '../../components/PlaceAutocomplete'
import { cities } from './cityData'
import { buildCityFacts } from './components/cityFactGenerators'

export function CityFolderPage() {
  const navigate = useNavigate()

  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">Property</h1>
      <p className="text-[1.1rem] text-muted-700">Two example cities, written up for the Phase 1 UI concept — a first draft, not final.</p>

      <div className="mt-6">
        {/* Jumps straight to the matching city/area/locality page — a
            DB-first search across all three levels, per §18. */}
        <PlaceAutocomplete
          scope="property"
          placeholder="Search a city, area, or locality…"
          onSelect={match => match.to && navigate(match.to)}
        />
      </div>

      <div className="mt-6 grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-4.5">
        {cities.map(city => (
          <FolderCard key={city.id} Icon={BuildingOffice2Icon} label={city.name} image={city.coverImage} to={`/property/cities/${city.id}`} />
        ))}
      </div>

      <FactsBoard facts={buildCityFacts(cities)} note="Computed from this app's own illustrative cost-of-living data — not live listings." />
    </SectionPanel>
  )
}
