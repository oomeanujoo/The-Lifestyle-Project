import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SettingsPage } from './SettingsPage'
import type { MasterRefreshOutcome, MasterStatus } from './masterRefreshApi'

const BASE = '/api/integration/v1/masters/lifestyle'
const statuses: MasterStatus[] = [
  { masterName: 'city', recordCount: 5, lastRefreshedAt: null, lastStatus: null },
  { masterName: 'currency', recordCount: 3, lastRefreshedAt: '2026-09-16T12:00:00Z', lastStatus: 'SUCCESS' },
]

function json(body: unknown, status = 200): Response {
  return { ok: status >= 200 && status < 300, status, json: async () => body } as Response
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(done => { resolve = done })
  return { promise, resolve }
}

const fetchMock = vi.fn<typeof fetch>()

function stubEndpoints(options: {
  statusResponse?: () => Promise<Response>
  currencyResponse?: () => Promise<Response>
  refreshResponse?: () => Promise<Response>
} = {}) {
  fetchMock.mockImplementation(async (input, init) => {
    const path = String(input)
    if (path === `${BASE}/refresh-status` && !init?.method) {
      return options.statusResponse ? options.statusResponse() : json(statuses)
    }
    if (path === `${BASE}/currencies` && !init?.method) {
      return options.currencyResponse ? options.currencyResponse() : json(['USD', 'INR', 'EUR'])
    }
    if (path === `${BASE}/refresh` && init?.method === 'POST') {
      return options.refreshResponse ? options.refreshResponse() : json([])
    }
    throw new Error(`Unexpected request: ${init?.method ?? 'GET'} ${path}`)
  })
}

async function loaded() {
  await screen.findByRole('list', { name: 'Shared masters' })
  await screen.findByRole('button', { name: 'INR' })
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  localStorage.clear()
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('Settings shared masters', () => {
  it('loads one Integration list and one currency list, preserving the display preference', async () => {
    localStorage.setItem('lifestyle:currency', 'USD')
    stubEndpoints()
    render(<SettingsPage />)
    await loaded()

    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      `${BASE}/refresh-status`, `${BASE}/currencies`,
    ])
    const list = screen.getByRole('list', { name: 'Shared masters' })
    expect(within(list).getAllByRole('listitem')).toHaveLength(2)
    expect(within(list).getByText('5 records')).toBeInTheDocument()
    expect(within(list).getByText('Last refreshed: Never refreshed')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Travel' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Property' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'USD' })).toHaveAttribute('aria-pressed', 'true')

    fireEvent.click(screen.getByRole('button', { name: 'EUR' }))
    expect(localStorage.getItem('lifestyle:currency')).toBe('EUR')
    expect(screen.getByRole('button', { name: 'EUR' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('shows loading separately from genuinely empty results', async () => {
    const statusRequest = deferred<Response>()
    const currencyRequest = deferred<Response>()
    stubEndpoints({
      statusResponse: () => statusRequest.promise,
      currencyResponse: () => currencyRequest.promise,
    })
    render(<SettingsPage />)
    expect(screen.getByText('Loading shared masters...')).toBeInTheDocument()
    expect(screen.getByText('Loading currencies...')).toBeInTheDocument()
    expect(screen.queryByText(/No currencies loaded yet/)).not.toBeInTheDocument()

    await act(async () => {
      statusRequest.resolve(json([]))
      currencyRequest.resolve(json([]))
    })
    expect(await screen.findByText('No shared masters reported by Integration yet.')).toBeInTheDocument()
    expect(screen.getByText(/No currencies loaded yet/)).toBeInTheDocument()
  })

  it('sends one POST, shows all outcomes, disables duplicate refreshes, and reloads both GETs', async () => {
    const refreshRequest = deferred<Response>()
    const outcomes: MasterRefreshOutcome[] = [
      { masterName: 'city', status: 'FAILED', recordsUpserted: 0, errorMessage: 'GeoNames: username not configured' },
      { masterName: 'currency', status: 'PARTIAL', recordsUpserted: 3, errorMessage: 'Frankfurter: unsupported currency AED' },
      { masterName: 'example', status: 'SUCCESS', recordsUpserted: 1, errorMessage: null },
    ]
    stubEndpoints({ refreshResponse: () => refreshRequest.promise })
    render(<SettingsPage />)
    await loaded()
    fireEvent.click(screen.getByRole('button', { name: 'Refresh master data' }))

    const pendingButton = screen.getByRole('button', { name: 'Refreshing...' })
    expect(pendingButton).toBeDisabled()
    fireEvent.click(pendingButton)
    expect(fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST')).toHaveLength(1)

    await act(async () => { refreshRequest.resolve(json(outcomes)) })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Refresh master data' })).toBeEnabled())
    const results = screen.getByRole('region', { name: 'Refresh results' })
    expect(within(results).getByText(/city: failed \(0 saved\).*GeoNames: username not configured/)).toBeInTheDocument()
    expect(within(results).getByText(/currency: partial \(3 saved\).*Frankfurter: unsupported currency AED/)).toBeInTheDocument()
    expect(within(results).getByText('example: success (1 saved)')).toBeInTheDocument()
    expect(fetchMock.mock.calls.map(([path, init]) => [path, init?.method ?? 'GET'])).toEqual([
      [`${BASE}/refresh-status`, 'GET'],
      [`${BASE}/currencies`, 'GET'],
      [`${BASE}/refresh`, 'POST'],
      [`${BASE}/refresh-status`, 'GET'],
      [`${BASE}/currencies`, 'GET'],
    ])
  })

  it('shows status HTTP failures while retaining successfully loaded currencies', async () => {
    stubEndpoints({ statusResponse: async () => json({}, 503) })
    render(<SettingsPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent(`GET ${BASE}/refresh-status failed with 503`)
    expect(await screen.findByRole('button', { name: 'EUR' })).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Shared masters' })).not.toBeInTheDocument()
  })

  it('shows a currency network failure rather than an empty-data message', async () => {
    stubEndpoints({ currencyResponse: async () => { throw new Error('Network unavailable') } })
    render(<SettingsPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent("Couldn't load currencies from Integration: Network unavailable")
    expect(await screen.findByRole('list', { name: 'Shared masters' })).toBeInTheDocument()
    expect(screen.queryByText(/No currencies loaded yet/)).not.toBeInTheDocument()
  })

  it('shows refresh HTTP failure, reloads data, and allows a retry', async () => {
    stubEndpoints({ refreshResponse: async () => json({}, 502) })
    render(<SettingsPage />)
    await loaded()
    fireEvent.click(screen.getByRole('button', { name: 'Refresh master data' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(`POST ${BASE}/refresh failed with 502`)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Refresh master data' })).toBeEnabled())
    expect(fetchMock.mock.calls.filter(([path]) => path === `${BASE}/refresh-status`)).toHaveLength(2)
    expect(fetchMock.mock.calls.filter(([path]) => path === `${BASE}/currencies`)).toHaveLength(2)

    fireEvent.click(screen.getByRole('button', { name: 'Refresh master data' }))
    await waitFor(() => expect(fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST')).toHaveLength(2))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Refresh master data' })).toBeEnabled())
  })

  it('reports an empty refresh response without claiming success', async () => {
    stubEndpoints()
    render(<SettingsPage />)
    await loaded()
    fireEvent.click(screen.getByRole('button', { name: 'Refresh master data' }))
    expect(await screen.findByText('Integration returned no refresh outcomes.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Refresh master data' })).toBeEnabled())
  })
})