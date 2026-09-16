import { Eyebrow } from '../../components/Eyebrow'

export function DashboardHero() {
  return (
    <section
      className="min-h-[335px] rounded-[22px] px-[7%] py-17 text-white"
      style={{
        background:
          'linear-gradient(110deg,rgba(14,57,59,.96),rgba(20,81,72,.82)),radial-gradient(circle at 85% 15%,#acbb89,transparent 45%)',
      }}
    >
      <Eyebrow tone="light">PLAN WITH PERSPECTIVE</Eyebrow>
      <h1 className="my-4.5 font-display text-[clamp(2.6rem,5vw,4.3rem)] leading-[1.08] font-semibold">
        Make room for what matters.
      </h1>
      <p className="max-w-[570px] text-[1.08rem] leading-relaxed text-[#e5efea]">
        One calm place to shape your next journey and your next place to call home.
      </p>
    </section>
  )
}
