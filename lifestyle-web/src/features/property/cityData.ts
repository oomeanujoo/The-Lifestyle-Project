export type CostLine = { label: string; monthlyINR: number }

export type NearbyPlace = { name: string; category: string }

export type City = {
  id: string
  name: string
  coverImage: string
  rentRangeINR: [number, number]
  costOfLiving: CostLine[]
  nearby: NearbyPlace[]
}

// Placeholder comparison content for the Phase 1 UI concept — costs are
// illustrative, manually entered, NOT sourced or live. See
// docs/product-concept-ui.md. Cover images hotlinked from Wikimedia Commons.
export const cities: City[] = [
  {
    id: 'pune',
    name: 'Pune',
    coverImage: 'https://commons.wikimedia.org/wiki/Special:FilePath/Pune_Skyline_2018.jpg?width=1600',
    rentRangeINR: [15000, 35000],
    costOfLiving: [
      { label: 'Rent (1BHK)', monthlyINR: 22000 },
      { label: 'Utilities', monthlyINR: 2500 },
      { label: 'Food', monthlyINR: 9000 },
      { label: 'Local transport', monthlyINR: 2000 },
    ],
    nearby: [
      { name: 'Vaishali', category: 'Food joint' },
      { name: 'German Bakery', category: 'Cafe' },
      { name: 'Koregaon Park', category: 'Neighborhood' },
    ],
  },
  {
    id: 'bengaluru',
    name: 'Bengaluru',
    coverImage: 'https://commons.wikimedia.org/wiki/Special:FilePath/Bangalore_Panorama.jpg?width=1600',
    rentRangeINR: [20000, 45000],
    costOfLiving: [
      { label: 'Rent (1BHK)', monthlyINR: 28000 },
      { label: 'Utilities', monthlyINR: 2800 },
      { label: 'Food', monthlyINR: 10000 },
      { label: 'Local transport', monthlyINR: 2500 },
    ],
    nearby: [
      { name: 'Vidyarthi Bhavan', category: 'Food joint' },
      { name: 'Third Wave Coffee', category: 'Cafe' },
      { name: 'Indiranagar', category: 'Neighborhood' },
    ],
  },
]

export const findCity = (id: string | undefined) => cities.find(city => city.id === id)
