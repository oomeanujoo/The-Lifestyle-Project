import { useEffect, useRef, useState } from 'react'
import { ChevronLeftIcon, ChevronRightIcon, SparklesIcon } from '@heroicons/react/24/outline'

// A fact is just a sentence — the emoji lives inline inside the text
// wherever it reads naturally, not pinned as a separate leading icon.
export type Fact = { text: string }

const shuffle = <T,>(items: T[]): T[] => {
  const copy = [...items]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

const VISIBLE = 3
const STEP_MS = 6000
const TRANSITION_MS = 1000

// Generic "Did you know?" pod — reused by Travel and Property, only the
// fact list differs. A true conveyor: one fact at a time slides out on the
// left, the rest shift up, a new one slides in on the right, on an
// infinite loop — not a discrete "page 1 of 3" swap. Order is randomized
// once per visit. Arrows step by one (same motion as auto-advance), which
// is what makes them feel smooth instead of a jarring whole-block swap.
export function FactsBoard({ facts, note }: { facts: Fact[]; note: string }) {
  const [ordered] = useState(() => shuffle(facts))
  const loopable = ordered.length > VISIBLE
  const track = loopable ? [...ordered, ...ordered.slice(0, VISIBLE)] : ordered

  const [index, setIndex] = useState(0)
  const [smooth, setSmooth] = useState(true)
  const timerRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined)

  const restart = () => {
    clearInterval(timerRef.current)
    if (loopable) timerRef.current = setInterval(() => setIndex(i => i + 1), STEP_MS)
  }

  useEffect(() => {
    restart()
    return () => clearInterval(timerRef.current)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- intentionally only depends on loopable
  }, [loopable])

  // Once the track has slid past the real facts into the cloned tail, snap
  // back to the start with the transition briefly switched off — the
  // standard trick for a carousel that loops forever without a visible cut.
  useEffect(() => {
    if (!loopable || index < ordered.length) return
    const resetAfterSlide = setTimeout(() => {
      setSmooth(false)
      setIndex(0)
      requestAnimationFrame(() => requestAnimationFrame(() => setSmooth(true)))
    }, TRANSITION_MS)
    return () => clearTimeout(resetAfterSlide)
  }, [index, loopable, ordered.length])

  if (ordered.length === 0) return null

  const go = (delta: number) => {
    setIndex(i => (delta < 0 && i === 0 ? ordered.length - 1 : i + delta))
    restart()
  }

  return (
    <div className="mt-8 rounded-2xl border border-line bg-white p-5">
      <div className="mb-4 flex items-center justify-between">
        <p className="flex items-center gap-2 font-display text-[1.05rem] font-semibold text-brand-950">
          <SparklesIcon className="size-5 text-brand-700" aria-hidden="true" /> Did you know?
        </p>
        {loopable && (
          <div className="flex items-center gap-2">
            <button type="button" onClick={() => go(-1)} aria-label="Previous fact" className="grid size-8 place-items-center rounded-full border border-line hover:border-brand-700">
              <ChevronLeftIcon className="size-4" aria-hidden="true" />
            </button>
            <button type="button" onClick={() => go(1)} aria-label="Next fact" className="grid size-8 place-items-center rounded-full border border-line hover:border-brand-700">
              <ChevronRightIcon className="size-4" aria-hidden="true" />
            </button>
          </div>
        )}
      </div>

      <div className="overflow-hidden">
        <ul
          className="flex"
          style={{
            transform: `translateX(-${index * (100 / VISIBLE)}%)`,
            transition: smooth ? `transform ${TRANSITION_MS}ms cubic-bezier(0.65,0,0.35,1)` : 'none',
          }}
        >
          {track.map((fact, i) => (
            <li key={i} className="w-1/3 shrink-0 px-1.5">
              <div className="flex min-h-24 items-center rounded-xl bg-brand-50 p-4 text-[.9rem] text-ink">{fact.text}</div>
            </li>
          ))}
        </ul>
      </div>

      <p className="mt-3 text-[.76rem] text-muted-500">{note}</p>
    </div>
  )
}
