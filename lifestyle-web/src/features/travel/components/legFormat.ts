import type { LegOption, RouteLeg, RouteOption, TransportMode } from '../tripData'

export const modeLabel: Record<TransportMode, string> = { flight: 'Flight', train: 'Train', road: 'Road' }
export const formatFare = (amount: number) => `₹${amount.toLocaleString('en-IN')}`
export const formatDuration = (hours: number) => (hours < 1 ? `${Math.round(hours * 60)}m` : `${hours}h`)

export type SortCriterion = 'fare' | 'duration'
export type Selection = Record<number, LegOption>

const bestOption = (leg: RouteLeg, sortBy: SortCriterion): LegOption =>
  [...leg.options].sort((a, b) => (sortBy === 'fare' ? a.fareINR - b.fareINR : a.durationHours - b.durationHours))[0]

// The rule the user asked for: sorting by fare auto-picks the cheapest mode
// on every leg (not just reorders the buttons); sorting by duration
// auto-picks the fastest. This is that pick, per leg.
export const autoPick = (route: RouteOption, sortBy: SortCriterion): Selection =>
  Object.fromEntries(route.legs.map((leg, index) => [index, bestOption(leg, sortBy)]))

export const totalsFor = (route: RouteOption, selection: Selection) =>
  route.legs.reduce(
    (totals, _leg, index) => {
      const option = selection[index]
      return option
        ? { fareINR: totals.fareINR + option.fareINR, hours: totals.hours + option.durationHours }
        : totals
    },
    { fareINR: 0, hours: 0 },
  )
