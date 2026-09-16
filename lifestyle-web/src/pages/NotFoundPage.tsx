import { Eyebrow } from '../components/Eyebrow'
import { SectionPanel } from '../components/SectionPanel'

export function NotFoundPage() {
  return (
    <SectionPanel>
      <Eyebrow>THE LIFESTYLE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">Not found</h1>
      <p className="text-[1.1rem] text-muted-700">That page doesn't exist yet.</p>
    </SectionPanel>
  )
}
