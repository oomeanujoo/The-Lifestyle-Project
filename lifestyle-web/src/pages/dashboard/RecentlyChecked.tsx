import { Link } from 'react-router'
import { PaperAirplaneIcon, BuildingOffice2Icon } from '@heroicons/react/24/outline'
import { readLastAccessed } from '../../lib/lastAccessed'

// Sits below the hero, in the exact spot the old "Your overview" folder
// grid used to occupy — this replaced it, it isn't an overlay on the hero.
// Navigating to a section is the top nav's job; this is a quick way back
// to the last thing you were looking at.
export function RecentlyChecked() {
  const lastTravel = readLastAccessed('travel')
  const lastProperty = readLastAccessed('property')

  return (
    <section aria-labelledby="recent-title">
      <div className="mt-13 mb-4.5 flex items-baseline justify-between">
        <h2 id="recent-title" className="font-display text-[1.85rem] font-semibold">Last checked</h2>
        <span className="text-[.9rem] text-[#738481]">Phase 1 workspace</span>
      </div>
      {!lastTravel && !lastProperty ? (
        <p className="text-muted-700">Nothing opened yet — visit Travel or Property to see it show up here.</p>
      ) : (
        <div className="flex flex-wrap gap-3">
          {lastTravel && (
            <Link to="/travel" className="flex items-center gap-2 rounded-full border border-line bg-white px-4 py-2 text-[.9rem] text-ink no-underline hover:border-brand-700">
              <PaperAirplaneIcon className="size-4 text-brand-700" aria-hidden="true" /> {lastTravel}
            </Link>
          )}
          {lastProperty && (
            <Link to="/property" className="flex items-center gap-2 rounded-full border border-line bg-white px-4 py-2 text-[.9rem] text-ink no-underline hover:border-brand-700">
              <BuildingOffice2Icon className="size-4 text-brand-700" aria-hidden="true" /> {lastProperty}
            </Link>
          )}
        </div>
      )}
    </section>
  )
}
