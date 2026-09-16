import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'
import App from './App'
import { EntryGate } from './components/EntryGate'
import './styles.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={new QueryClient()}>
      <EntryGate>
        <BrowserRouter><App /></BrowserRouter>
      </EntryGate>
    </QueryClientProvider>
  </React.StrictMode>,
)
