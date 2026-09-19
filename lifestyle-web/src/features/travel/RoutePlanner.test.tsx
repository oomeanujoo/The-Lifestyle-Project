import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RoutePlanner } from './RoutePlanner'
import { destinationPhoto, recommendRoad, suggestCities, type RoadJourney } from './routePlanningApi'

vi.mock('./routePlanningApi', () => ({ suggestCities: vi.fn(), recommendRoad: vi.fn(), destinationPhoto: vi.fn() }))
const point = (label: string) => ({ id: label, label, countryCode: 'IN', latitude: 1, longitude: 2, source: 'GeoNames', asOf: null })
const route: RoadJourney = {
  origin: point('Pune'), destination: point('Gwalior'), via: null,
  vias: [], suggestedViaCandidates: [],
  straightLineDistanceKm: 800, straightLineLabel: 'Straight-line only',
  roadRoute: { distanceKm: 950, durationMinutes: 960, geometry: null, source: 'OSRM demo', capturedAt: '2026-09-19T00:00:00Z' },
  roadRouteUnavailableReason: null,
  fuelCostEstimate: { amount: 4750, currency: 'INR', fuelPricePerLitre: 100, vehicleKmPerLitre: 20, label: 'Fuel estimate only' },
}
const show = () => render(<MemoryRouter><RoutePlanner /></MemoryRouter>)
const add = (city: string) => {
  fireEvent.change(screen.getByRole('textbox'), { target: { value: city } })
  fireEvent.keyDown(screen.getByRole('textbox'), { key: 'Enter' })
}
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(suggestCities).mockResolvedValue([])
  vi.mocked(recommendRoad).mockResolvedValue(route)
  vi.mocked(destinationPhoto).mockResolvedValue(null)
})
afterEach(cleanup)

describe('RoutePlanner', () => {
  it('has one input and forms From, To and Via chips in sequence', () => {
    show()
    expect(screen.getAllByRole('textbox')).toHaveLength(1)
    add('Pune')
    expect(screen.getByRole('button', { name: 'Edit From: Pune' })).toBeInTheDocument()
    add('Gwalior')
    expect(screen.getByRole('button', { name: 'Edit To: Gwalior' })).toBeInTheDocument()
    add('Aurangabad')
    expect(screen.getByRole('button', { name: 'Edit Via 1: Aurangabad' })).toBeInTheDocument()
    expect(screen.getAllByRole('textbox')).toHaveLength(1)
  })
  it('shows live city suggestions from backend', async () => {
    vi.mocked(suggestCities).mockResolvedValue(['Mumbai'])
    show()
    fireEvent.focus(screen.getByRole('textbox'))
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Mum' } })
    await waitFor(() => expect(suggestCities).toHaveBeenCalledWith('Mum'))
    fireEvent.click(await screen.findByRole('button', { name: 'Mumbai' }))
    expect(screen.getByRole('button', { name: 'Edit From: Mumbai' })).toBeInTheDocument()
  })
  it('requires From and To', () => {
    show()
    fireEvent.click(screen.getByRole('button', { name: 'Find route' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Add a From city and a To city first.')
    expect(recommendRoad).not.toHaveBeenCalled()
  })
  it('shows an image card with a details link and real road data', async () => {
    show()
    add('Pune')
    add('Gwalior')
    fireEvent.click(screen.getByRole('button', { name: 'Find route' }))
    expect(await screen.findByRole('link', { name: 'Open route details for Pune to Gwalior' })).toHaveAttribute('href', '/travel/routes/detail?origin=Pune&destination=Gwalior')
    expect(recommendRoad).toHaveBeenCalledExactlyOnceWith('Pune', 'Gwalior', [])
    expect(screen.getByText(/Road route · 950 km · about 16h 0m/)).toBeInTheDocument()
    expect(screen.getByText(/Fuel estimate: 4,750 INR/)).toBeInTheDocument()
  })
  it('passes optional Via in one request', async () => {
    vi.mocked(recommendRoad).mockResolvedValue({ ...route, via: point('Aurangabad'), vias: [point('Aurangabad')] })
    show()
    add('Pune')
    add('Gwalior')
    add('Aurangabad')
    fireEvent.click(screen.getByRole('button', { name: 'Find route' }))
    expect(await screen.findByRole('link', { name: 'Open route details for Pune to Gwalior' })).toHaveAttribute('href', '/travel/routes/detail?origin=Pune&destination=Gwalior&via=Aurangabad')
    expect(recommendRoad).toHaveBeenCalledExactlyOnceWith('Pune', 'Gwalior', ['Aurangabad'])
  })
  it('passes multiple via cities in order and keeps suggested via cities', async () => {
    vi.mocked(recommendRoad).mockResolvedValue({ ...route, vias: [point('Mumbai'), point('Delhi')],
      suggestedViaCandidates: [{ via: point('Indore'), originToViaKm: 1, viaToDestinationKm: 2, totalKm: 3, detourKm: 4 }] })
    show()
    add('Pune')
    add('Gwalior')
    add('Mumbai')
    add('Delhi')
    fireEvent.click(screen.getByRole('button', { name: 'Find route' }))
    expect(await screen.findByRole('button', { name: /Via Indore/ })).toBeInTheDocument()
    expect(recommendRoad).toHaveBeenCalledExactlyOnceWith('Pune', 'Gwalior', ['Mumbai', 'Delhi'])
    fireEvent.click(screen.getByRole('button', { name: /Via Indore/ }))
    expect(screen.getByRole('button', { name: 'Edit Via 3: Indore' })).toBeInTheDocument()
  })
  it('honestly labels fallback', async () => {
    vi.mocked(recommendRoad).mockResolvedValue({ ...route, roadRoute: null, fuelCostEstimate: null, roadRouteUnavailableReason: 'provider unavailable' })
    show()
    add('Pune')
    add('Gwalior')
    fireEvent.click(screen.getByRole('button', { name: 'Find route' }))
    expect(await screen.findByText(/Straight-line comparison · 800 km/)).toBeInTheDocument()
    expect(screen.getByText(/Road route unavailable: provider unavailable/)).toBeInTheDocument()
  })
})
