import { formatDuration, formatFare } from './legFormat'

export function FareSummary({ totalFareINR, totalHours }: { totalFareINR: number; totalHours: number }) {
  return (
    <aside className="sticky top-4 rounded-[14px] bg-brand-100 p-5">
      <p className="text-[.75rem] font-bold tracking-[.2em] text-brand-500 uppercase">TOTAL</p>
      <p className="my-1 font-sans text-[1.7rem] font-bold tabular-nums text-brand-950">{formatFare(totalFareINR)}</p>
      <p className="mb-2.5 text-muted-700 tabular-nums">{formatDuration(totalHours)} total travel time</p>
      <p className="text-[.76rem] text-muted-500">Illustrative fares, manually entered — not live prices.</p>
    </aside>
  )
}
