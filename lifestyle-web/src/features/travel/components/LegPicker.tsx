import { useMemo } from 'react'
import type { LegOption, RouteLeg } from '../tripData'
import { formatDuration, formatFare, modeLabel } from './legFormat'

export function LegPicker({ leg, sortBy, selected, onSelect }: {
  leg: RouteLeg
  sortBy: 'fare' | 'duration'
  selected: LegOption
  onSelect: (option: LegOption) => void
}) {
  const options = useMemo(
    () => [...leg.options].sort((a, b) => (sortBy === 'fare' ? a.fareINR - b.fareINR : a.durationHours - b.durationHours)),
    [leg, sortBy],
  )

  return (
    <li className="flex flex-col items-start gap-2 rounded-xl border border-line bg-white p-3.5">
      <span className="font-bold text-ink">{leg.from} &rarr; {leg.to}</span>
      <div className="flex flex-wrap gap-2">
        {options.map(option => (
          <button
            key={option.mode}
            type="button"
            onClick={() => onSelect(option)}
            className={`rounded-full border px-3.5 py-2 text-[.88rem] ${
              option.mode === selected.mode ? 'border-brand-700 bg-brand-700 text-white' : 'border-line bg-white'
            }`}
          >
            {modeLabel[option.mode]} &middot; <span className="font-sans tabular-nums">{formatFare(option.fareINR)}</span> &middot; <span className="font-sans tabular-nums">{formatDuration(option.durationHours)}</span>
          </button>
        ))}
      </div>
    </li>
  )
}
