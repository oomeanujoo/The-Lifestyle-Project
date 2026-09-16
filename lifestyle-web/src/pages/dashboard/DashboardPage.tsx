import { DashboardHero } from './DashboardHero'
import { RecentlyChecked } from './RecentlyChecked'

// Hero + a compact "last checked" recap, in the spot the old Travel/Property
// overview grid used to be — nav (with icons) already covers getting to a
// section, so the Dashboard stays short enough to fit one view.
export function DashboardPage() {
  return (
    <>
      <DashboardHero />
      <RecentlyChecked />
    </>
  )
}
