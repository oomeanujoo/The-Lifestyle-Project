import { Eyebrow } from '../../components/Eyebrow'
import { SectionPanel } from '../../components/SectionPanel'
import { RoutePlanner } from './RoutePlanner'

// Previously also rendered a static "illustrative trips" section (hardcoded
// fare/time facts, a second PlaceAutocomplete search box filtering them,
// and a FactsBoard computed from that static data) — removed per explicit
// takeover instruction: one prominent, real route search, no second search
// box, no hardcoded cards presented as if they were live results.
export function TravelFolderPage() {
  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">Travel</h1>
      <p className="text-[1.1rem] text-muted-700">Plan a route using live place data.</p>
      <RoutePlanner />
    </SectionPanel>
  )
}
