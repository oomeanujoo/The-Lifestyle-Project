import { useState } from 'react'
import { formatFare } from './legFormat'

const todayISO = () => new Date().toISOString().slice(0, 10)
const tomorrowISO = () => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}

// Depart/return dates and one-way vs. round-trip, with the going/returning
// cost shown separately as well as combined — same illustrative fare data
// mirrored for the return leg (no independent return pricing modeled yet).
export function TripDates({ oneWayFareINR }: { oneWayFareINR: number }) {
  const [tripType, setTripType] = useState<'one-way' | 'round-trip'>('round-trip')
  const [departDate, setDepartDate] = useState(todayISO())
  const [returnDate, setReturnDate] = useState(tomorrowISO())

  return (
    <div className="mb-5 rounded-xl border border-line bg-white p-4">
      <div className="mb-3 flex gap-2">
        <button
          type="button"
          onClick={() => setTripType('one-way')}
          className={`rounded-full border px-3.5 py-1.5 text-[.85rem] ${tripType === 'one-way' ? 'border-brand-700 bg-brand-700 text-white' : 'border-line bg-white'}`}
        >
          One-way
        </button>
        <button
          type="button"
          onClick={() => setTripType('round-trip')}
          className={`rounded-full border px-3.5 py-1.5 text-[.85rem] ${tripType === 'round-trip' ? 'border-brand-700 bg-brand-700 text-white' : 'border-line bg-white'}`}
        >
          Round trip
        </button>
      </div>

      <div className="flex flex-wrap items-end gap-4">
        <label className="flex flex-col gap-1 text-[.82rem] text-muted-600">
          Depart
          <input type="date" value={departDate} min={todayISO()} onChange={e => setDepartDate(e.target.value)} className="rounded-lg border border-line px-2.5 py-1.5 font-sans tabular-nums" />
        </label>
        {tripType === 'round-trip' && (
          <label className="flex flex-col gap-1 text-[.82rem] text-muted-600">
            Return
            <input type="date" value={returnDate} min={departDate} onChange={e => setReturnDate(e.target.value)} className="rounded-lg border border-line px-2.5 py-1.5 font-sans tabular-nums" />
          </label>
        )}
      </div>

      {tripType === 'round-trip' && (
        <div className="mt-4 flex flex-wrap gap-6 border-t border-line pt-3 text-[.92rem]">
          <span>Going ({departDate}): <strong className="font-sans tabular-nums">{formatFare(oneWayFareINR)}</strong></span>
          <span>Returning ({returnDate}): <strong className="font-sans tabular-nums">{formatFare(oneWayFareINR)}</strong></span>
          <span>Combined: <strong className="font-sans tabular-nums text-brand-950">{formatFare(oneWayFareINR * 2)}</strong></span>
        </div>
      )}
      <p className="mt-2 text-[.76rem] text-muted-500">Return leg mirrors the same route and fares — not independently priced yet.</p>
    </div>
  )
}
