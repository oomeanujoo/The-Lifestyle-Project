import { useState } from 'react'
import { DataRow } from '../../../components/DataRow'
import type { ItineraryDay } from '../tripData'

// Itinerary is optional — hidden by default, shown on request. See the
// decision log for why (early feedback: "itinerary is something optional").
export function ItinerarySection({ days }: { days: ItineraryDay[] }) {
  const [show, setShow] = useState(false)

  return (
    <div>
      <div className="mt-8 flex items-center justify-between">
        <h2 className="font-display text-[1.3rem] font-semibold">Itinerary</h2>
        <button
          type="button"
          onClick={() => setShow(v => !v)}
          className="rounded-full border border-brand-700 px-4 py-2 text-[.86rem] font-bold text-brand-700 hover:bg-brand-100"
        >
          {show ? 'Remove itinerary' : 'Add itinerary'}
        </button>
      </div>
      {show && (
        <ol className="mt-3 flex flex-col gap-2.5">
          {days.map(day => (
            <DataRow key={day.day} leading={`Day ${day.day}`} title={day.title} detail={day.detail} />
          ))}
        </ol>
      )}
    </div>
  )
}
