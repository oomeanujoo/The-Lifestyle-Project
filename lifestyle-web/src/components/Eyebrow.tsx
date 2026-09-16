// Small uppercase label used above headings throughout the app. `tone`
// picks the variant for a dark hero background vs. a light section.
export function Eyebrow({ children, tone = 'brand' }: { children: string; tone?: 'brand' | 'light' }) {
  return (
    <p className={`text-[.75rem] font-bold tracking-[.2em] uppercase ${tone === 'light' ? 'text-[#bdcfc2]' : 'text-brand-500'}`}>
      {children}
    </p>
  )
}
