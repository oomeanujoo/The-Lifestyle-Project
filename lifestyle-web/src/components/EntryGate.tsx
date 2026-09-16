import { useState, type ReactNode } from 'react'
import confetti from 'canvas-confetti'

// No shape at all — a `clip-path` diamond, even blurred, still reads as a
// diamond because blur only softens the outermost edge pixels, it doesn't
// remove the underlying silhouette. A radial gradient has no boundary to
// begin with, which is what actually makes something look like a spread
// of soft light rather than an object flashing on screen.
const FLASH_GLOW = 'radial-gradient(circle, rgba(255,255,255,.9) 0%, rgba(255,255,255,.5) 18%, rgba(255,255,255,.18) 40%, rgba(255,255,255,0) 68%)'

// Ambient flashes on the sides — plenty of big "softbox" ones now, not
// just a couple, firing often (short durations, tightly packed delays) so
// it reads as continuous paparazzi rather than an occasional glow.
const AMBIENT_FLASHES = [
  { side: 'left', top: '8%', size: 360, duration: 5, delay: 0, peak: 0.2 },
  { side: 'left', top: '34%', size: 150, duration: 3, delay: 0.7, peak: 0.3 },
  { side: 'left', top: '58%', size: 400, duration: 6, delay: 1.4, peak: 0.18 },
  { side: 'left', top: '84%', size: 220, duration: 4, delay: 0.3, peak: 0.26 },
  { side: 'right', top: '12%', size: 320, duration: 4.5, delay: 1, peak: 0.22 },
  { side: 'right', top: '38%', size: 170, duration: 3.2, delay: 0.5, peak: 0.3 },
  { side: 'right', top: '62%', size: 420, duration: 6.5, delay: 1.9, peak: 0.16 },
  { side: 'right', top: '88%', size: 130, duration: 3.6, delay: 1.2, peak: 0.28 },
  { side: 'left', top: '95%', size: 280, duration: 5.5, delay: 2.2, peak: 0.2 },
] as const

// Tone-setting entry screen, not age verification — see
// docs/product-concept-ui.md §1. A red-carpet-premiere moment: one soft,
// shapeless glow spreads on mount, the logo/heading/subtext/button reveal
// as it fades (not before), then faint ambient glows continue very
// subtly on the sides. Deliberate one-off exception to the brand palette
// — see the decision log.
//
// `children` is always rendered, with the gate as a fixed overlay on top
// that fades out — so the app underneath is already painted before the
// gate disappears, avoiding the white-flash gap an unmount/mount sequence
// used to leave.
export function EntryGate({ children }: { children: ReactNode }) {
  const [showGate, setShowGate] = useState(true)
  const [leaving, setLeaving] = useState(false)

  const enter = () => {
    confetti({ particleCount: 140, spread: 100, origin: { y: 0.6 }, colors: ['#ffffff', '#d4af7a', '#e8e2d0'] })
    setLeaving(true)
    setTimeout(() => setShowGate(false), 300)
  }

  return (
    <>
      {children}
      {showGate && (
        <div
          className={`fixed inset-0 z-50 flex flex-col items-center justify-center overflow-hidden bg-[#0b0b0d] p-6 text-center transition-opacity duration-300 ${leaving ? 'opacity-0' : 'opacity-100'}`}
        >
          <span
            aria-hidden="true"
            className="animate-hero-flash pointer-events-none absolute size-[42rem] rounded-full blur-2xl"
            style={{ background: FLASH_GLOW }}
          />

          {AMBIENT_FLASHES.map((flash, i) => (
            <span
              key={i}
              aria-hidden="true"
              className={`animate-camera-flash pointer-events-none absolute rounded-full blur-2xl ${flash.side === 'left' ? 'left-[4%]' : 'right-[4%]'}`}
              style={{
                top: flash.top,
                width: flash.size,
                height: flash.size,
                animationDuration: `${flash.duration}s`,
                animationDelay: `${flash.delay}s`,
                background: FLASH_GLOW,
                ['--flash-peak' as string]: flash.peak,
              }}
            />
          ))}

          <div className="animate-entry-reveal relative z-10 flex flex-col items-center gap-5">
            <span className="relative grid size-20 place-items-center rounded-2xl bg-brand-950 ring-2 ring-white/40">
              <span aria-hidden="true" className="absolute inset-0 animate-pulse rounded-2xl ring-2 ring-[#d4af7a]/60" />
              <span className="font-serif text-[2.4rem] text-white">L.</span>
            </span>
            <h1 className="font-display text-[clamp(2.6rem,7vw,4.2rem)] leading-[1.05] font-semibold text-white [text-shadow:0_0_30px_rgba(212,175,122,0.35)]">
              The Lifestyle
            </h1>
            <p className="max-w-md text-[1.1rem] text-white/70">You're entering a new era of planning. Your next trip. Your next city. Zero chaos, all vibes.</p>
            <button
              type="button"
              onClick={enter}
              className="mt-4 rounded-full bg-white px-9 py-4 text-base font-bold tracking-wide text-[#0b0b0d] transition-transform hover:scale-105"
            >
              🎉 yep, I'm 18+ &mdash; let's go &rarr;
            </button>
          </div>
        </div>
      )}
    </>
  )
}
