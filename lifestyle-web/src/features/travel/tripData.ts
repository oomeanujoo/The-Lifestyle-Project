export type TransportMode = 'flight' | 'train' | 'road'

export type LegOption = { mode: TransportMode; fareINR: number; durationHours: number }

export type RouteLeg = { from: string; to: string; options: LegOption[] }

export type RouteOption = { id: string; label: string; legs: RouteLeg[] }

export type ItineraryDay = { day: number; title: string; detail: string }

export type Trip = {
  id: string
  title: string
  coverImage: string
  coverLabel: string
  routes: RouteOption[]
  itinerary: ItineraryDay[]
}

// Placeholder planning content for the Phase 1 UI concept — routes, fares
// and durations are illustrative, manually entered, NOT sourced or live.
// See docs/product-concept-ui.md. Cover images are hotlinked from Wikimedia
// Commons via its Special:FilePath mechanism (no scraping, no upload/hosting
// needed) — verify they load in a real browser; this sandbox has no outbound
// network access to check.
export const trips: Trip[] = [
  {
    id: 'pune-gwalior',
    title: 'Pune → Gwalior → Pune',
    coverImage: 'https://commons.wikimedia.org/wiki/Special:FilePath/Gwalior_Fort_Gwalior.JPG?width=1600',
    coverLabel: 'Gwalior Fort',
    routes: [
      {
        id: 'via-mumbai',
        label: 'Via Mumbai',
        legs: [
          {
            from: 'Pune', to: 'Mumbai',
            options: [
              { mode: 'road', fareINR: 450, durationHours: 3.5 },
              { mode: 'train', fareINR: 350, durationHours: 4 },
            ],
          },
          {
            from: 'Mumbai', to: 'Gwalior',
            options: [
              { mode: 'flight', fareINR: 4200, durationHours: 1.5 },
              { mode: 'train', fareINR: 900, durationHours: 15 },
            ],
          },
        ],
      },
      {
        id: 'via-delhi',
        label: 'Via Delhi',
        legs: [
          {
            from: 'Pune', to: 'Delhi',
            options: [
              { mode: 'flight', fareINR: 3800, durationHours: 2 },
              { mode: 'train', fareINR: 1200, durationHours: 20 },
            ],
          },
          {
            from: 'Delhi', to: 'Gwalior',
            options: [
              { mode: 'train', fareINR: 450, durationHours: 3 },
              { mode: 'road', fareINR: 600, durationHours: 5 },
            ],
          },
        ],
      },
    ],
    itinerary: [
      { day: 1, title: 'Arrival', detail: 'Travel in, check-in, evening walk near the fort area.' },
      { day: 2, title: 'Gwalior Fort', detail: 'Full day at the fort complex and Man Mandir Palace.' },
      { day: 3, title: 'City sights', detail: 'Jai Vilas Palace, Sarod House, local markets.' },
      { day: 4, title: 'Return', detail: 'Travel back to Pune.' },
    ],
  },
  {
    id: 'pune-dubai',
    title: 'Pune → Dubai → Pune',
    coverImage: 'https://commons.wikimedia.org/wiki/Special:FilePath/Dubai_Skyline_and_Burj_Khalifa_-_25072008.jpg?width=1600',
    coverLabel: 'Dubai Skyline',
    routes: [
      {
        id: 'direct',
        label: 'Direct flight',
        legs: [
          {
            from: 'Pune', to: 'Dubai',
            options: [{ mode: 'flight', fareINR: 18000, durationHours: 3.5 }],
          },
        ],
      },
    ],
    itinerary: [
      { day: 1, title: 'Arrival', detail: 'Check-in, evening at Dubai Marina.' },
      { day: 2, title: 'Downtown', detail: 'Burj Khalifa and Dubai Mall.' },
      { day: 3, title: 'Old Dubai', detail: 'Gold Souk, Spice Souk, abra ride across the Creek.' },
      { day: 4, title: 'Desert safari', detail: 'Afternoon/evening safari, free morning.' },
      { day: 5, title: 'Departure', detail: 'Travel back to Pune.' },
    ],
  },
]

export const findTrip = (id: string | undefined) => trips.find(trip => trip.id === id)
