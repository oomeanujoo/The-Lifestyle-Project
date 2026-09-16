import { NavLink } from 'react-router'
import { Nav } from './Nav'

export function Header() {
  return (
    <header className="border-b border-[#e3e7e4] bg-white">
      <div className="mx-auto flex min-h-[82px] max-w-6xl items-center justify-between gap-7 px-7">
        <NavLink to="/" aria-label="The Lifestyle home" className="flex items-center gap-3 whitespace-nowrap text-xl font-bold no-underline">
          <span className="grid size-10 place-items-center rounded-xl bg-brand-950 font-serif text-[1.35rem] text-white">L.</span>
          <span className="font-display">The Lifestyle</span>
        </NavLink>
        <Nav />
      </div>
    </header>
  )
}
