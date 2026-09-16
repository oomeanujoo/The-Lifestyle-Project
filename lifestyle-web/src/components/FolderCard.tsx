import { useState, type ComponentType, type SVGProps } from 'react'
import { useNavigate } from 'react-router'

// Generic double-click-to-open folder tile, reused for top-level sections
// (Travel/Property) and individual trip/city folders — only the
// data/icon/image/to changes per use, not the component. See
// TECHNICAL_ARCHITECTURE.md §10 (generic over bespoke). `Icon` is a
// Heroicons component, not emoji — see the decision log entry on
// switching to one consistent icon library.
export function FolderCard({ Icon, label, sublabel, image, to }: {
  Icon: ComponentType<SVGProps<SVGSVGElement>>
  label: string
  sublabel?: string
  image?: string
  to: string
}) {
  const [selected, setSelected] = useState(false)
  const navigate = useNavigate()

  const base = 'flex flex-col items-center gap-1.5 overflow-hidden rounded-2xl p-6 text-center font-[inherit] shadow-[0_8px_26px_#233b2d08] cursor-pointer'
  // Plain (icon) cards get a visible border for definition; image cards get
  // none at all — a border + border-radius + background-image is what was
  // causing the hairline seam at the rounded corners, so image cards skip
  // borders entirely rather than using a transparent one.
  const plain = `border-2 bg-white border-line hover:border-brand-700 ${selected ? 'border-brand-900 bg-brand-100' : ''}`
  const withImage = 'min-h-[200px] items-start justify-end bg-cover bg-center bg-no-repeat bg-clip-padding text-left'

  return (
    <button
      type="button"
      className={`${base} ${image ? withImage : plain}`}
      style={image ? { backgroundImage: `linear-gradient(180deg, rgba(20,63,64,.15), rgba(20,63,64,.85)), url("${image}")` } : undefined}
      onClick={() => setSelected(true)}
      onDoubleClick={() => navigate(to)}
      aria-label={`${label} folder — double-click to open`}
    >
      {!image && <Icon className="size-8 text-brand-700" aria-hidden="true" />}
      <span className={`font-bold ${image ? 'text-lg text-white' : 'text-ink'}`}>{label}</span>
      {sublabel && <span className={`text-[.82rem] ${image ? 'text-[#dfe9e4]' : 'text-muted-600'}`}>{sublabel}</span>}
      <span className={`text-[.78rem] ${image ? 'text-[#dfe9e4]' : 'text-muted-500'}`}>Double-click to open</span>
    </button>
  )
}
