import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('application shell', () => {
  it('shows the dashboard hero and the main navigation', () => {
    render(<QueryClientProvider client={new QueryClient()}><MemoryRouter><App /></MemoryRouter></QueryClientProvider>)
    expect(screen.getByRole('heading', { name: 'Make room for what matters.' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Travel' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Property' })).toBeInTheDocument()
  })
})
