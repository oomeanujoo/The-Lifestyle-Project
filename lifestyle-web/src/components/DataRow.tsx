// Generic bordered row — reused for itinerary days, nearby places, and
// cost-of-living lines. Same shape everywhere; only the text differs.
export function DataRow({ leading, title, detail, trailing }: {
  leading?: string
  title: string
  detail?: string
  trailing?: string
}) {
  return (
    <li className="flex flex-wrap items-baseline gap-3 rounded-xl border border-line bg-white px-4 py-3">
      {leading && <span className="min-w-16 font-bold text-brand-700">{leading}</span>}
      <span className="font-semibold">{title}</span>
      {detail && <span className="basis-full text-[.92rem] text-muted-600">{detail}</span>}
      {trailing && <span className="ml-auto font-sans text-[1.1rem] font-bold tabular-nums text-ink">{trailing}</span>}
    </li>
  )
}
