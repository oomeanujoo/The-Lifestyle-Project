import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { TravelFolderPage } from './TravelFolderPage'

// RoutePlanner makes its own real fetch calls (mocked/tested separately in
// RoutePlanner.test.tsx) — stub it here so this file only asserts what
// TravelFolderPage itself renders.
vi.mock('./RoutePlanner', () => ({
  RoutePlanner: () => <div data-testid="route-planner-stub" />,
}))

afterEach(cleanup)

describe('TravelFolderPage', () => {
  it('renders exactly one route search and nothing else', () => {
    render(<TravelFolderPage />)
    expect(screen.getByTestId('route-planner-stub')).toBeInTheDocument()
  })

  it('never shows the illustrative trip cards or their static facts', () => {
    render(<TravelFolderPage />)
    expect(screen.queryByText(/Illustrative trips/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/not route-search results/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/illustrative fares and times/i)).not.toBeInTheDocument()
  })

  it('never shows a second place-search box', () => {
    render(<TravelFolderPage />)
    expect(screen.queryByPlaceholderText(/Search a place across your trips/i)).not.toBeInTheDocument()
  })
})
