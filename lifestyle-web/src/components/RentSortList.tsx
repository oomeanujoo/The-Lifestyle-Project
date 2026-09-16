import { useState, type ComponentType, type SVGProps } from 'react'
import { FolderCard } from './FolderCard'

export type RentListItem = { id: string; label: string; sublabel: string; avgRentINR: number; to: string }

// Generic sortable-by-rent folder grid — reused for the area list within a
// city and the locality list within an area, same shape either level.
export function RentSortList({ items, Icon }: { items: RentListItem[]; Icon: ComponentType<SVGProps<SVGSVGElement>> }) {
  const [descending, setDescending] = useState(true)
  const sorted = [...items].sort((a, b) => (descending ? b.avgRentINR - a.avgRentINR : a.avgRentINR - b.avgRentINR))

  return (
    <div>
      <div className="mb-4 flex items-center gap-2 text-[.88rem] text-muted-600">
        <span>Sort by rent</span>
        <button
          type="button"
          onClick={() => setDescending(true)}
          className={`rounded-full border px-3 py-1.25 text-[.82rem] ${descending ? 'border-brand-700 bg-brand-100 font-bold text-brand-900' : 'border-line bg-white'}`}
        >
          High to low
        </button>
        <button
          type="button"
          onClick={() => setDescending(false)}
          className={`rounded-full border px-3 py-1.25 text-[.82rem] ${!descending ? 'border-brand-700 bg-brand-100 font-bold text-brand-900' : 'border-line bg-white'}`}
        >
          Low to high
        </button>
      </div>
      <div className="grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-4.5">
        {sorted.map(item => (
          <FolderCard key={item.id} Icon={Icon} label={item.label} sublabel={item.sublabel} to={item.to} />
        ))}
      </div>
    </div>
  )
}
