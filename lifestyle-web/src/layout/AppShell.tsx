import type { ReactNode } from 'react'
import { useLocation } from 'react-router'
import { Header } from './Header'
import { Footer } from './Footer'

// The Dashboard ("/") is pinned to exactly one viewport height with no
// scroll at all — everything else scrolls normally (a fixed height would
// clip real content on longer pages, which was the earlier mistake).
export function AppShell({ children }: { children: ReactNode }) {
  const isDashboard = useLocation().pathname === '/'

  return (
    <div className={`flex flex-col ${isDashboard ? 'h-dvh overflow-hidden' : 'min-h-screen'}`}>
      <Header />
      <main className={`mx-auto w-full max-w-6xl flex-1 px-7 ${isDashboard ? 'flex flex-col justify-center py-6' : 'pt-11 pb-20'}`}>
        {children}
      </main>
      {!isDashboard && <Footer />}
    </div>
  )
}
