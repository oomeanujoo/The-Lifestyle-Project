# ADR-005: Immutable price snapshots

Status: accepted. Store each captured quote with currency, travel dates when relevant, source, provider, timestamp, confidence and expiration. Compare snapshots without overwriting old estimates. Manual/imported prices are first-class when free live APIs are unavailable. Refresh policy may be 15 days, 30 days or manual only; no scheduler in Phase 1.
