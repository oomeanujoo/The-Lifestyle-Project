import { useState } from 'react'
import { Navigate, useParams } from 'react-router'
import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { ADD_ON_SERVICES, findArea, findLocality, type BhkType } from './areaData'

const money = (amount: number) => `₹${amount.toLocaleString('en-IN')}`

export function LocalityDetailPage() {
  const { cityId, areaId, localityId } = useParams()
  const area = findArea(cityId, areaId)
  const locality = findLocality(cityId, areaId, localityId)
  const [bhk, setBhk] = useState<BhkType | undefined>(locality?.bhkRents[0]?.type)
  const [addOns, setAddOns] = useState<string[]>([])

  if (!area || !locality) return <Navigate to="/property" replace />

  const bhkRent = locality.bhkRents.find(b => b.type === bhk)
  const addOnTotal = ADD_ON_SERVICES.filter(s => addOns.includes(s.id)).reduce((sum, s) => sum + s.monthlyINR, 0)
  const total = (bhkRent?.avgRentINR ?? 0) + addOnTotal

  const toggleAddOn = (id: string) => setAddOns(prev => (prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]))

  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.2rem,4.5vw,3.4rem)] leading-[1.08] font-semibold">{locality.name}</h1>
      <p className="text-[1.05rem] text-muted-700">{area.name} · {area.municipality} · PIN {locality.pincode}</p>

      <h2 className="mt-8 mb-3 font-display text-[1.3rem] font-semibold">Choose your home size</h2>
      <div className="flex flex-wrap gap-2">
        {locality.bhkRents.map(rent => (
          <button
            key={rent.type}
            type="button"
            onClick={() => setBhk(rent.type)}
            className={`rounded-full border px-4 py-2 ${rent.type === bhk ? 'border-brand-950 bg-brand-950 text-white' : 'border-line bg-white'}`}
          >
            {rent.type} &middot; <span className="font-sans tabular-nums">{money(rent.avgRentINR)}/mo</span>
          </button>
        ))}
      </div>

      <h2 className="mt-8 mb-3 font-display text-[1.3rem] font-semibold">Add-on services</h2>
      <div className="flex flex-col gap-2">
        {ADD_ON_SERVICES.map(service => (
          <label key={service.id} className="flex cursor-pointer items-center justify-between rounded-xl border border-line bg-white p-4">
            <span className="flex items-center gap-2.5">
              <input
                type="checkbox"
                checked={addOns.includes(service.id)}
                onChange={() => toggleAddOn(service.id)}
                className="size-4 accent-brand-700"
              />
              {service.label}
            </span>
            <span className="font-sans font-bold tabular-nums">{money(service.monthlyINR)}/mo</span>
          </label>
        ))}
      </div>

      <div className="mt-8 rounded-2xl bg-brand-50 p-5">
        <p className="text-[.75rem] font-bold tracking-[.2em] text-brand-500 uppercase">Estimated monthly cost</p>
        <p className="my-1 font-sans text-[1.7rem] font-bold tabular-nums text-brand-950">{money(total)}</p>
        <p className="text-[.76rem] text-muted-500">Illustrative rent + add-ons, manually entered — not a real listing.</p>
      </div>
    </SectionPanel>
  )
}
