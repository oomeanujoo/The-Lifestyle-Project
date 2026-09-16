import { Fragment } from 'react'
import type { RouteOption } from '../tripData'
import { formatDuration, formatFare, modeLabel, type Selection } from './legFormat'

// A clean, vintage-parchment journey trail — NOT a real geographic map (no
// map-tile library needed or wanted), and deliberately understated: aged
// paper via a soft inset vignette (no literal sticker illustrations, which
// read as cartoonish rather than "old"), a single faint compass watermark,
// and the flag marker on its own with no badge/circle around it. Segment
// length is proportional to that leg's duration (a 15h leg draws a longer
// line than a 4h leg), and the first/last flags sit flush at the frame's
// edges rather than floating in the middle.
export function JourneyMap({ route, selection }: { route: RouteOption; selection: Selection }) {
  const waypoints = [route.legs[0]?.from, ...route.legs.map(leg => leg.to)].filter(Boolean) as string[]
  const chosen = route.legs.map((leg, index) => selection[index] ?? leg.options[0])

  return (
    <div
      className="relative mt-6 overflow-hidden rounded-2xl border border-brand-500/30 bg-brand-50 p-6 shadow-[inset_0_0_50px_rgba(20,63,64,0.12)]"
    >
      <span
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-[9rem] text-brand-700 opacity-[0.05] select-none"
      >
        &#9678;
      </span>

      <p className="relative mb-6 font-display text-[1.05rem] font-semibold tracking-[0.08em] text-brand-950 uppercase">
        The route, mapped
      </p>

      <div className="relative flex w-full items-start">
        {waypoints.map((place, index) => (
          <Fragment key={place + index}>
            <div className="flex shrink-0 flex-col items-center">
              <span className="text-2xl" aria-hidden="true">🚩</span>
              <span className="mt-1 max-w-24 text-center font-display text-xs font-semibold tracking-wide text-brand-950 uppercase">
                {place}
              </span>
            </div>

            {index < waypoints.length - 1 && (
              <div
                className="flex flex-col items-center px-1 pt-3.5"
                style={{ flexGrow: chosen[index].durationHours, flexBasis: 40, flexShrink: 1 }}
              >
                <span className="w-full border-t border-dotted border-brand-700/70" />
                <span className="mt-1.5 text-center text-[.78rem] text-muted-700">
                  {modeLabel[chosen[index].mode]}
                  <br />
                  <span className="font-sans font-bold tabular-nums text-brand-900">{formatFare(chosen[index].fareINR)}</span>
                  {' · '}
                  <span className="tabular-nums">{formatDuration(chosen[index].durationHours)}</span>
                </span>
              </div>
            )}
          </Fragment>
        ))}
      </div>
    </div>
  )
}
