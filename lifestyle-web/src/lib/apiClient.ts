// Generic JSON fetch wrapper — every backend call in this app goes through
// this one function, per the generic-over-bespoke rule (§10). Relative
// paths only (`/api/travel/...`), so Vite's dev-server proxy (vite.config.ts)
// routes to the right backend without this code needing to know a port.
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) throw new ApiError(response.status, `GET ${path} failed with ${response.status}`)
  return response.json() as Promise<T>
}

export async function apiPost<T>(path: string): Promise<T> {
  const response = await fetch(path, { method: 'POST' })
  if (!response.ok) throw new ApiError(response.status, `POST ${path} failed with ${response.status}`)
  return response.json() as Promise<T>
}
