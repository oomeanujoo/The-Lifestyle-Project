import { DashboardHero } from './DashboardHero'
import { RecentlyChecked } from './RecentlyChecked'
import { AiStatusWidget } from '../../features/dashboard/AiStatusWidget'

// Hero + a compact "last checked" recap, in the spot the old Travel/Property
// overview grid used to be — nav (with icons) already covers getting to a
// section, so the Dashboard stays short enough to fit one view. AiStatusWidget
// sits below both, one compact row, and renders nothing at all if
// integration-service isn't reachable — never breaks the page.
export function DashboardPage() {
  return (
    <>
      <DashboardHero />
      <RecentlyChecked />
      <AiStatusWidget />
    </>
  )
}
