# Data strategy

## Primary store and ownership

PostgreSQL remains the only Phase 1 data store. Travel and Property own separate schemas and migrations. Planned relational tables hold stable domain facts, keys, constraints and relationships: plans, destinations, itinerary days, travel legs, city comparisons, costs, quotes and user-reviewed suggestions. Services communicate through APIs, never cross-schema queries or foreign keys.

## Flexible metadata and provenance

Future provider adapters may retain selected raw external metadata in PostgreSQL `jsonb` beside normalized columns. Canonical fields needed for filtering or arithmetic—amount, ISO currency, source/provider, travel dates, capture time, expiration and verification status—remain typed columns. JSONB is for variable provider fields, not an excuse to hide the domain model. Validate payload shape and size, record a provider schema version, and redact tokens, personal data and unnecessary raw responses before storage.

Every imported or API-sourced fact records provider name, source URL or import file reference, capture time, observation dates, license/attribution where relevant, confidence and verification status. Manual entries carry a manual source marker and editor timestamp. AI metadata adds model/provider, prompt version, generation timestamp, referenced data IDs/versions, verification status and user acceptance status. Unverified AI output stays a draft.

## Price history and retention

Price snapshots are append-only observations. Corrections create a new observation or explicit supersession link; they do not silently rewrite prior quotes. Java code computes totals, percentage changes, sorting and route scores from typed decimal amounts, currencies and timestamps. Cross-currency comparisons require a sourced exchange-rate observation and its date. Historical displays label stale data and the snapshot time.

Phase 1 stores only migration markers. Before collecting real plan or provider data, define retention windows per data class and user export/deletion behavior. Planned baseline: keep user plan and accepted suggestion data until the user deletes it; retain price history while a plan exists; expire temporary raw provider payloads sooner than normalized provenance when licensing and debugging needs permit. Backups and logs must follow the same deletion policy. Avoid storing secrets in any payload. No automatic purge job is added in Phase 1.

## Deferred storage options

- **pgvector:** consider only after a RAG evaluation shows PostgreSQL full-text search misses a measurable relevance target and embedding operations fit local memory/disk budgets.
- **PostGIS:** consider when real geospatial queries, not just map display or route-service responses, require indexed spatial operations at a measured scale.
- **Separate NoSQL database:** rejected for now. The bounded contexts have relational ownership, transactions and constrained comparisons; JSONB covers provider variation. A second database would add backup, consistency and operational costs without an observed requirement.
- **Redis, Kafka, Elasticsearch and MinIO:** no Phase 1 role. Revisit only against concrete latency, throughput, search or object-storage measurements that PostgreSQL and filesystem-backed exports cannot satisfy.

No pgvector, PostGIS, MongoDB, Redis, Kafka, Elasticsearch or MinIO dependency or container is installed in Phase 1.
