import { HomeIcon } from '@heroicons/react/24/outline'
import { Navigate, useParams } from 'react-router'
import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { RentSortList } from '../../components/RentSortList'
import { findArea, localityAvgRent } from './areaData'

const money = (amount: number) => `₹${amount.toLocaleString('en-IN')}/mo avg`

export function LocalityListPage() {
  const { cityId, areaId } = useParams()
  const area = findArea(cityId, areaId)
  if (!area) return <Navigate to="/property" replace />

  const items = area.localities.map(locality => ({
    id: locality.id,
    label: locality.name,
    sublabel: `PIN ${locality.pincode} · ${money(localityAvgRent(locality))}`,
    avgRentINR: localityAvgRent(locality),
    to: `/property/cities/${cityId}/areas/${areaId}/localities/${locality.id}`,
  }))

  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">{area.name}</h1>
      <p className="text-[1.1rem] text-muted-700">{area.municipality} · localities inside {area.name}, illustrative only.</p>
      <div className="mt-6">
        <RentSortList items={items} Icon={HomeIcon} />
      </div>
    </SectionPanel>
  )
}
