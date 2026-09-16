import type { ReactNode } from 'react'

// Generic sage rounded panel — the shape every "page inside a section"
// (Travel/Property folder lists, trip/city detail pages, not-found) uses.
export function SectionPanel({ children }: { children: ReactNode }) {
  return <section className="min-h-[360px] rounded-[22px] bg-brand-100 px-[7%] py-16">{children}</section>
}
