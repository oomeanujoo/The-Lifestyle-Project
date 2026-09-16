// Generic "last opened" tracker, reused for Travel trips and Property
// cities — one small localStorage-backed helper instead of one per section.
export type Section = 'travel' | 'property'

const key = (section: Section) => `lifestyle:lastAccessed:${section}`

export function recordAccess(section: Section, label: string) {
  try {
    localStorage.setItem(key(section), label)
  } catch {
    // ignore — best-effort only
  }
}

export function readLastAccessed(section: Section): string | null {
  try {
    return localStorage.getItem(key(section))
  } catch {
    return null
  }
}
