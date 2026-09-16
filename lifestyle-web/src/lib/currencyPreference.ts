// Selected display currency is a per-browser preference, not a saved
// "account setting" — this app has no accounts (§20). Same localStorage
// pattern as lastAccessed.ts.
const KEY = 'lifestyle:currency'

export function readCurrency(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function saveCurrency(isoCode: string) {
  try {
    localStorage.setItem(KEY, isoCode)
  } catch {
    // ignore — best-effort only
  }
}
