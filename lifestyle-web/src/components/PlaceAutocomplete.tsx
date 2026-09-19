import { useEffect, useId, useState } from 'react'
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline'
import { searchPlaces, AI_FALLBACK_NOTICE, type PlaceMatch } from '../lib/placeSearch'

// Generic search box — the frontend half of the DB-first / AI-fallback
// design in TECHNICAL_ARCHITECTURE.md §18. Reused by both Travel (a real,
// live lifestyle_master.city search, §13 decision log 2026-09-19) and
// Property (still static illustrative data — see placeSearch.ts for why)
// — only `scope` changes, per the generic-over-bespoke rule in §10.
// Debounced since Travel's search is now a real network request per
// keystroke otherwise.
export function PlaceAutocomplete({ scope, placeholder, onSelect }: {
  scope: 'travel' | 'property'
  placeholder: string
  onSelect: (match: PlaceMatch) => void
}) {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [results, setResults] = useState<PlaceMatch[]>([])
  const listId = useId()

  const trimmed = query.trim()
  const showResults = open && trimmed.length >= 2

  useEffect(() => {
    if (trimmed.length < 2) {
      setResults([])
      return
    }
    let active = true
    const timer = setTimeout(() => {
      searchPlaces(scope, trimmed)
        .then(matches => { if (active) setResults(matches) })
        .catch(() => { if (active) setResults([]) })
    }, 200)
    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [scope, trimmed])

  const pick = (match: PlaceMatch) => {
    onSelect(match)
    setQuery('')
    setOpen(false)
  }

  return (
    <div className="relative w-full max-w-sm">
      <div className="flex items-center gap-2 rounded-xl border-2 border-line bg-white px-4 py-2.5 focus-within:border-brand-700">
        <MagnifyingGlassIcon className="size-4 shrink-0 text-muted-500" aria-hidden="true" />
        <input
          type="text"
          value={query}
          placeholder={placeholder}
          onChange={event => {
            setQuery(event.target.value)
            setOpen(true)
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 120)}
          className="w-full bg-transparent text-sm text-ink outline-none placeholder:text-muted-500"
          aria-label={placeholder}
          aria-expanded={showResults}
          aria-controls={listId}
        />
      </div>

      {showResults && (
        <ul id={listId} className="absolute z-20 mt-1.5 w-full overflow-hidden rounded-xl border-2 border-line bg-white shadow-[0_8px_26px_#233b2d1a]">
          {results.map(match => (
            <li key={match.id}>
              <button
                type="button"
                onMouseDown={() => pick(match)}
                className="flex w-full flex-col items-start gap-0.5 px-4 py-2.5 text-left hover:bg-brand-100"
              >
                <span className="text-sm font-semibold text-ink">{match.label}</span>
                {match.sublabel && <span className="text-[.78rem] text-muted-600">{match.sublabel}</span>}
              </button>
            </li>
          ))}
          {results.length === 0 && (
            <li className="px-4 py-2.5 text-[.78rem] text-muted-600 italic">{AI_FALLBACK_NOTICE}</li>
          )}
        </ul>
      )}
    </div>
  )
}
