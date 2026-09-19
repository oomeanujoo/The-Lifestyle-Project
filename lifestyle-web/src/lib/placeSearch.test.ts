import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { searchPlaces } from './placeSearch'

const fetchMock = vi.fn<typeof fetch>()
function response(body: unknown, status = 200): Response {
  return { ok: status >= 200 && status < 300, status, json: async () => body } as Response
}
beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})
afterEach(() => vi.unstubAllGlobals())

describe('searchPlaces — travel scope', () => {
  it('returns nothing below the minimum query length, without calling the backend', async () => {
    expect(await searchPlaces('travel', 'P')).toEqual([])
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('calls the real travel-service backend and maps its response', async () => {
    fetchMock.mockResolvedValue(response({
      matches: [{ id: 'city-abc', label: 'Mumbai', sublabel: 'City · IN · GeoNames', kind: 'TRAVEL_PLACE' }],
      aiFallbackNotice: null,
    }))

    const result = await searchPlaces('travel', 'Mum')

    expect(fetchMock).toHaveBeenCalledExactlyOnceWith('/api/travel/v1/places/search?q=Mum')
    expect(result).toEqual([{ id: 'city-abc', label: 'Mumbai', sublabel: 'City · IN · GeoNames', kind: 'travel-place' }])
  })

  it('propagates a backend failure rather than substituting example places', async () => {
    fetchMock.mockResolvedValue(response({}, 503))
    await expect(searchPlaces('travel', 'Mum')).rejects.toThrow()
  })
})

describe('searchPlaces — property scope (still static, documented why)', () => {
  it('does not call the network at all', async () => {
    await searchPlaces('property', 'Pune')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('still finds a known illustrative city', async () => {
    const result = await searchPlaces('property', 'Pune')
    expect(result.some(match => match.label === 'Pune')).toBe(true)
  })
})
