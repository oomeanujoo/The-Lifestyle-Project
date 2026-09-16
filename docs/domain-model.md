# Planned domain model

These are design concepts, not Phase 1 tables. IDs crossing contexts are references handled through APIs.

## Travel ownership

`TripPlan` is the planning aggregate root. It owns ordered `Destination`, `ItineraryDay`, `TravelLeg` (`TransportMode` value), `BorderCrossing`, `Accommodation`, `Budget`, `ExpenseItem`, `TripNote` and `ChecklistItem` data. `DocumentRequirement` is a sourced requirement attached to a destination/crossing and requires verification. `PriceQuote` is a sourced observation; `PriceSnapshot` groups immutable quotes at a capture time so old versus current estimates remain comparable. `AiSuggestion` is a separate reviewable proposal with provenance and approval status. Detailed aggregate splits will be validated by actual Phase 2 use cases.

## Property ownership

`PropertyPlan` is the comparison aggregate root and references `CityProfile`, `AccommodationOption`, `PropertyListingReference`, `MonthlyLivingCost`, `CommuteOption`, `CityScore` and `BuyRentComparison`. `RentSnapshot` and `PropertyPriceSnapshot` are immutable observations owned by Property. `AiRecommendation` is a reviewable proposal, never an authoritative listing fact. City reference data may become a separate catalog within Property if use cases justify it.

Neither context owns the other's tables or imports its Java domain types. Currency amounts must preserve currency codes and observation dates. Prices, visa requirements and AI output require source and verification metadata.
