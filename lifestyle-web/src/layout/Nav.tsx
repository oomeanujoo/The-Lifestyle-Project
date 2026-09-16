import { NavLink } from 'react-router'
import { HomeIcon, PaperAirplaneIcon, BuildingOffice2Icon } from '@heroicons/react/24/outline'

// One icon library, one consistent minimal (line/outline) style — see the
// decision log entry on switching from mixed emoji to Heroicons.
const links = [
  { to: '/', label: 'Dashboard', Icon: HomeIcon, end: true },
  { to: '/travel', label: 'Travel', Icon: PaperAirplaneIcon },
  { to: '/property', label: 'Property', Icon: BuildingOffice2Icon },
]

export function Nav() {
  return (
    <nav aria-label="Main navigation" className="flex flex-wrap gap-2">
      {links.map(({ to, label, Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) =>
            `flex items-center gap-1.5 rounded-[9px] px-3.5 py-2.5 text-sm no-underline ${isActive ? 'bg-brand-100 text-brand-900' : 'text-muted-700'} hover:bg-brand-100 hover:text-brand-900`
          }
        >
          <Icon className="size-4" aria-hidden="true" />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}
