import { Route, Routes } from 'react-router'
import { AppShell } from './layout/AppShell'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { TravelFolderPage } from './features/travel/TravelFolderPage'
import { RouteDetailPage } from './features/travel/RouteDetailPage'
import { TripDetailPage } from './features/travel/TripDetailPage'
import { CityFolderPage } from './features/property/CityFolderPage'
import { CityDetailPage } from './features/property/CityDetailPage'
import { LocalityListPage } from './features/property/LocalityListPage'
import { LocalityDetailPage } from './features/property/LocalityDetailPage'
import { SettingsPage } from './features/settings/SettingsPage'

export default function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/travel" element={<TravelFolderPage />} />
        <Route path="/travel/routes/detail" element={<RouteDetailPage />} />
        <Route path="/travel/trips/:tripId" element={<TripDetailPage />} />
        <Route path="/property" element={<CityFolderPage />} />
        <Route path="/property/cities/:cityId" element={<CityDetailPage />} />
        <Route path="/property/cities/:cityId/areas/:areaId" element={<LocalityListPage />} />
        <Route path="/property/cities/:cityId/areas/:areaId/localities/:localityId" element={<LocalityDetailPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppShell>
  )
}
