export type BhkType = '1RK' | '1BHK' | '2BHK' | '3BHK' | '4BHK' | 'Villa'
export type BhkRent = { type: BhkType; avgRentINR: number }

export type Locality = {
  id: string
  name: string
  pincode: string
  bhkRents: BhkRent[]
}

export type Municipality = 'PMC' | 'PCMC'

export type Area = {
  id: string
  name: string
  municipality: Municipality
  localities: Locality[]
}

export type AddOnService = { id: string; label: string; monthlyINR: number }

// Illustrative only — manually entered, not sourced or live, matching the
// rest of this app's placeholder data (see docs/product-concept-ui.md).
// Only Pune has this depth of breakdown for now; other cities can get the
// same treatment later using this same shape.
export const ADD_ON_SERVICES: AddOnService[] = [
  { id: 'tiffin', label: 'Tiffin / meal service', monthlyINR: 3500 },
  { id: 'gym', label: 'Gym / fitness membership', monthlyINR: 1500 },
]

export const areasByCity: Record<string, Area[]> = {
  pune: [
    {
      id: 'koregaon-park',
      name: 'Koregaon Park',
      municipality: 'PMC',
      localities: [
        {
          id: 'kp-north-main-road',
          name: 'North Main Road',
          pincode: '411001',
          bhkRents: [
            { type: '1RK', avgRentINR: 14000 },
            { type: '1BHK', avgRentINR: 22000 },
            { type: '2BHK', avgRentINR: 34000 },
            { type: '3BHK', avgRentINR: 52000 },
          ],
        },
        {
          id: 'kp-lane-6',
          name: 'Lane 6',
          pincode: '411001',
          bhkRents: [
            { type: '1BHK', avgRentINR: 19000 },
            { type: '2BHK', avgRentINR: 29000 },
            { type: '3BHK', avgRentINR: 45000 },
          ],
        },
      ],
    },
    {
      id: 'aundh',
      name: 'Aundh',
      municipality: 'PMC',
      localities: [
        {
          id: 'aundh-gandhi-nagar',
          name: 'Gandhi Nagar',
          pincode: '411007',
          bhkRents: [
            { type: '1RK', avgRentINR: 10000 },
            { type: '1BHK', avgRentINR: 17000 },
            { type: '2BHK', avgRentINR: 26000 },
            { type: '3BHK', avgRentINR: 38000 },
          ],
        },
        {
          id: 'aundh-dp-road',
          name: 'DP Road',
          pincode: '411007',
          bhkRents: [
            { type: '1BHK', avgRentINR: 15000 },
            { type: '2BHK', avgRentINR: 23000 },
          ],
        },
      ],
    },
    {
      id: 'hinjewadi',
      name: 'Hinjewadi',
      municipality: 'PCMC',
      localities: [
        {
          id: 'hinjewadi-phase-1',
          name: 'Phase 1',
          pincode: '411057',
          bhkRents: [
            { type: '1BHK', avgRentINR: 13000 },
            { type: '2BHK', avgRentINR: 19000 },
            { type: '3BHK', avgRentINR: 27000 },
          ],
        },
        {
          id: 'hinjewadi-phase-2',
          name: 'Phase 2',
          pincode: '411057',
          bhkRents: [
            { type: '1RK', avgRentINR: 8000 },
            { type: '1BHK', avgRentINR: 12000 },
            { type: '2BHK', avgRentINR: 17500 },
          ],
        },
      ],
    },
  ],
}

const avg = (values: number[]) => Math.round(values.reduce((sum, v) => sum + v, 0) / values.length)

export const localityAvgRent = (locality: Locality) => avg(locality.bhkRents.map(b => b.avgRentINR))
export const areaAvgRent = (area: Area) => avg(area.localities.map(localityAvgRent))

export const findAreas = (cityId: string | undefined) => areasByCity[cityId ?? ''] ?? []
export const findArea = (cityId: string | undefined, areaId: string | undefined) =>
  findAreas(cityId).find(area => area.id === areaId)
export const findLocality = (cityId: string | undefined, areaId: string | undefined, localityId: string | undefined) =>
  findArea(cityId, areaId)?.localities.find(locality => locality.id === localityId)
