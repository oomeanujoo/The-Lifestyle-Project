import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { recommendRoad, recommendRoute, suggestCities } from './routePlanningApi'

const BASE = '/api/travel/v1/routes'
const fetchMock = vi.fn<typeof fetch>()
function response(body: unknown, status = 200): Response {
  return { ok: status >= 200 && status < 300, status, json: async () => body } as Response
}
beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})
afterEach(() => vi.unstubAllGlobals())

describe('route-planning client', () => {
  it('returns no suggestions for a query shorter than 2 characters, without calling the backend', async () => {
    expect(await suggestCities('M')).toEqual([])
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('fetches live city suggestions by prefix', async () => {
    const body = ['Mumbai', 'Mumbra']
    fetchMock.mockResolvedValue(response(body))
    expect(await suggestCities('Mu')).toEqual(body)
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${BASE}/city-suggestions?q=Mu`)
  })

  it('requests a direct route without a via parameter when none is given', async () => {
    const body = { origin: {}, destination: {}, directDistanceKm: 1, distanceLabel: '', requestedVia: null, suggestedViaCandidates: [] }
    fetchMock.mockResolvedValue(response(body))
    expect(await recommendRoute('Pune', 'Mumbai')).toEqual(body)
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${BASE}/recommend?origin=Pune&destination=Mumbai`)
  })

  it('includes via when provided', async () => {
    const body = { origin: {}, destination: {}, directDistanceKm: 1, distanceLabel: '', requestedVia: null, suggestedViaCandidates: [] }
    fetchMock.mockResolvedValue(response(body))
    await recommendRoute('Pune', 'Mumbai', 'Gwalior')
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${BASE}/recommend?origin=Pune&destination=Mumbai&via=Gwalior`)
  })

  it('surfaces the backend\'s own honest error message on a 400, not a generic HTTP status', async () => {
    fetchMock.mockResolvedValue(response({ error: "destination 'Atlantis' was not found in the refreshed lifestyle_master.city masters" }, 400))
    await expect(recommendRoute('Pune', 'Atlantis')).rejects.toThrow('Atlantis')
  })

  it('falls back to a generic HTTP message when the backend gives no error field', async () => {
    fetchMock.mockResolvedValue(response({}, 503))
    await expect(recommendRoute('Pune', 'Mumbai')).rejects.toThrow('Route recommendation failed with HTTP 503')
  })

  it('propagates network failure without substituting an example route', async () => {
    fetchMock.mockRejectedValue(new Error('Network unavailable'))
    await expect(recommendRoute('Pune', 'Mumbai')).rejects.toThrow('Network unavailable')
  })

  it('requests a real road route without a via parameter when none is given', async () => {
    const body = {
      origin: {}, destination: {}, via: null, straightLineDistanceKm: 1, straightLineLabel: '',
      roadRoute: null, roadRouteUnavailableReason: 'disabled', fuelCostEstimate: null,
    }
    fetchMock.mockResolvedValue(response(body))
    expect(await recommendRoad('Pune', 'Mumbai')).toEqual(body)
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${BASE}/recommend-road?origin=Pune&destination=Mumbai`)
  })

  it('includes via when provided for a road route request', async () => {
    const body = {
      origin: {}, destination: {}, via: {}, straightLineDistanceKm: 1, straightLineLabel: '',
      roadRoute: null, roadRouteUnavailableReason: 'disabled', fuelCostEstimate: null,
    }
    fetchMock.mockResolvedValue(response(body))
    await recommendRoad('Pune', 'Mumbai', 'Gwalior')
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${BASE}/recommend-road?origin=Pune&destination=Mumbai&via=Gwalior`)
  })

  it('sends multiple vias as ordered repeated parameters', async () => {
    fetchMock.mockResolvedValue(response({ vias: [] }))
    await recommendRoad('Pune', 'Gwalior', ['Mumbai', 'Delhi'])
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith(`${BASE}/recommend-road?origin=Pune&destination=Gwalior&via=Mumbai&via=Delhi`)
  })

  it('acquires a verified missing Indian city only after Find route and retries', async () => {
    fetchMock.mockResolvedValueOnce(response({ error: "destination 'Kohima' was not found in the refreshed lifestyle_master.city masters" }, 400))
      .mockResolvedValueOnce(response({ name: 'Kohima', source: 'GeoNames' }))
      .mockResolvedValueOnce(response({ origin: {}, destination: {} }))
    await recommendRoad('Pune', 'Kohima')
    expect(fetchMock).toHaveBeenNthCalledWith(2,
      '/api/integration/v1/masters/lifestyle/cities/acquire?name=Kohima&countryCode=IN', { method: 'POST' })
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('surfaces the road-route backend\'s own honest error message on a 400', async () => {
    fetchMock.mockResolvedValue(response({ error: "destination 'Atlantis' was not found in the refreshed lifestyle_master.city masters" }, 400))
    await expect(recommendRoad('Pune', 'Atlantis')).rejects.toThrow('Atlantis')
  })
})
