# Technical Architecture

> **Current implementation update, 2026-09-16:** The three services now target separate `travel`, `property`, and `integration` schemas in the existing local `postgres` database. Integration gained Flyway/JDBC storage for AI suggestion provenance and an Ollama-first provider using the optional Compose `ai` profile. Successful suggestions are stored as `UNVERIFIED`/`DRAFT` with provider, model, prompt version, generation time, and referenced data. The local 16 GB machine selected `qwen3:4b-instruct`; no model is pulled by Compose. Travel and Property still do not call Integration's AI endpoint. Older passages below describing Integration as database-free or this machine as lacking Java 25 are historical and superseded by this update. Backend tests/migrations remain unverified because the agent terminal's Gradle daemon failed before test execution with a Java loopback connection error; do not infer they pass.

How The Lifestyle is built, and why — written so anyone can follow it, no prior architecture background required. Terms are defined the first time they're used.

This is a personal, learning-driven project, not a client deliverable. It borrows patterns real teams use — bounded contexts, hexagonal architecture, ADRs, C4-style diagrams — to practice real engineering judgment at small scale, not to look impressive. Keep it developer-friendly and pragmatic over clever.

This is a living document, updated as things are built — not a log of every change. See [§13, decision log](#13-decision-log) for what earns an entry.

Deeper references: [docs/architecture.md](docs/architecture.md), [docs/domain-model.md](docs/domain-model.md), [docs/technology-versions.md](docs/technology-versions.md), [docs/product-concept-ui.md](docs/product-concept-ui.md), [ADRs](docs/decisions/).

> All diagrams are [Mermaid](https://mermaid.js.org/), one consistent dark palette, each scoped to a single concern and kept small on purpose.

**Diagram style guide** — which convention applies to which kind of diagram, used consistently below and for anything added later:

| Diagram is showing... | Style |
|---|---|
| Full project architecture | Layered layout, rounded card nodes, nested containers, orthogonal connectors |
| A single feature's flow | Rounded nodes, top-to-bottom, curved connectors |
| Microservice internals | Layered component diagram, nested containers, orthogonal connectors |
| A user journey | Rounded cards, curved connectors (Miro/flowmapp style) |
| Database design | Flat entities, orthogonal relationship lines (true ER diagram once real tables exist) |
| Website structure / sitemap | Hierarchical, rounded nodes, top-to-bottom |
| Rough brainstorming | Free-form whiteboard, sticky notes |

Page-design mockups (Figma-style) and presentation/infographic polish are intentionally out of scope for this document — see [§2](#2-who-this-is-for) for why (no Artifact without explicit permission).

---

## 1. What this project is

A tool for planning two kinds of big life decisions: **travel trips** and **comparing cities to live in**. **Local-first** — runs entirely on your machine, no cloud account. **AI-optional** — AI can draft plans, but never invents facts like prices or visas; a human always approves before it becomes real data.

Currently **Phase 1**: the foundation (independent apps, isolated database schemas, a working web shell) is built. Trip and property planning themselves are Phase 2, not yet implemented. See [docs/roadmap.md](docs/roadmap.md).

---

## 2. Who this is for

**The problem:** trip and city-comparison planning today is scattered across tabs, notes, and screenshots that don't talk to each other. Nothing remembers a price change, nothing compares two routes side by side, nothing gives a plan one calm home.

**Persona — the Independent Planner:** one person (today, the project's own builder), planning their own trips and future moves. Comfortable with technology, wants one durable place to hold a plan, not a generic form-heavy dashboard. Desktop-first. Tone: calm and editorial — matching the app's own copy, *"Make room for what matters."*

**User stories**
- Hold an entire trip — route, stops, transport, budget — in one place.
- Compare two route alternatives (via Mumbai vs. via Delhi) side by side before committing.
- See where every price/estimate came from and when, to know whether to trust it.
- Let AI draft a plan from a description, but never let it become "the truth" without review.
- Compare cities to live in with the same rigor as trips.

**Site map** — drawn as a sticky-note board rather than Mermaid's native `mindmap` type (which failed to render for some viewers): each branch is one outlined, single-border group; the yellow tiles inside are the "ideas stuck to the board," not separate boxes with their own crossing lines.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}}}%%
flowchart LR
    Root(("🧭 The<br/>Lifestyle"))

    Root --> TravelGroup
    Root --> PropertyGroup
    Root -.-> AIGroup

    subgraph TravelGroup["✈️ Travel"]
        direction TB
        T1["🗂️ Trip folder"]
        T2["🔀 Route alternatives"]
        T3["📅 Itinerary and budget"]
    end

    subgraph PropertyGroup["🏠 Property"]
        direction TB
        P1["🏙️ City folder"]
        P2["💰 Cost of living"]
        P3["🏡 Accommodation options"]
    end

    subgraph AIGroup["🤖 AI Planner"]
        A1["Draft only —<br/>never authoritative"]
    end

    class TravelGroup travelBorder;
    class PropertyGroup propertyBorder;
    class AIGroup aiBorder;
    class T1,T2,T3,P1,P2,P3,A1 sticky;

    classDef travelBorder fill:transparent,stroke:#5eead4,color:#ecfeff,stroke-width:2px,rx:12,ry:12;
    classDef propertyBorder fill:transparent,stroke:#e9d5ff,color:#faf5ff,stroke-width:2px,rx:12,ry:12;
    classDef aiBorder fill:transparent,stroke:#fde68a,color:#fffbeb,stroke-width:2px,stroke-dasharray:4 4,rx:12,ry:12;
    classDef sticky fill:#fde047,stroke:#1c1917,color:#1c1917,stroke-width:1.5px,rx:2,ry:2;
```

**Primary flow** (full concept and worked examples: [docs/product-concept-ui.md](docs/product-concept-ui.md))

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'basis'}}}%%
flowchart LR
    A(["🔐 Entry gate"]) --> B(["🖥️ Dashboard"])
    B --> C(["✈️ Travel"])
    B --> D(["🏠 Property"])
    C --> E(["🗂️ Pune → Gwalior → Pune"])
    D --> F(["🏙️ City comparison"])

    class A,B neutral;
    class C,E travel;
    class D,F property;

    classDef neutral fill:#334155,stroke:#94a3b8,color:#f1f5f9,stroke-width:2px,rx:10,ry:10;
    classDef travel fill:#0e7490,stroke:#67e8f9,color:#ecfeff,stroke-width:2px,rx:10,ry:10;
    classDef property fill:#7e22ce,stroke:#e9d5ff,color:#faf5ff,stroke-width:2px,rx:10,ry:10;
```

**Existing brand — reuse, don't redesign.** `lifestyle-web/src/styles.css` already has a real identity: the "L." logomark (deep teal-green, serif), Playfair Display + DM Sans. Palette:

| Swatch | Hex | Used for |
|---|---|---|
| ![](https://placehold.co/32x16/143f40/143f40.png) | `#143f40` | Logomark, primary tone |
| ![](https://placehold.co/32x16/1c685c/1c685c.png) | `#1c685c` | Links, interactive text |
| ![](https://placehold.co/32x16/21745b/21745b.png) | `#21745b` | Success/status-good |
| ![](https://placehold.co/32x16/eaf0e8/eaf0e8.png) | `#eaf0e8` | Section background |
| ![](https://placehold.co/32x16/f8f6f1/f8f6f1.png) | `#f8f6f1` | App background |

---

## 3. Information architecture

How the product's content and navigation are organized — separate from the visual polish in [§2](#2-who-this-is-for). Everything below is **built and real** in `lifestyle-web` (frontend only — see [§12](#12-where-the-project-is-today) for what's still missing on the backend side); nothing here is the proposed/dashed concept it used to be earlier in the project.

### 3.1 Sitemap / page hierarchy

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}}}%%
flowchart TB
    Gate(("Entry gate")) --> Dashboard["Dashboard"]
    Dashboard --> Travel["Travel"]
    Dashboard --> Property["Property"]

    Travel --> TripFolder["Trip folder<br/>e.g. Pune → Gwalior → Pune"]
    TripFolder --> TripDetail["Route comparison ·<br/>journey map · itinerary"]

    Property --> City["City folder<br/>e.g. Pune"]
    City --> Area["Area<br/>e.g. Aundh"]
    Area --> Locality["Locality<br/>e.g. Gandhi Nagar"]
    Locality --> LocalityDetail["BHK size · add-ons ·<br/>estimated total cost"]

    Dashboard --> Settings["Settings"]

    class Dashboard,Gate,Settings neutral;
    class Travel,TripFolder,TripDetail travel;
    class Property,City,Area,Locality,LocalityDetail property;

    classDef neutral fill:#334155,stroke:#94a3b8,color:#f1f5f9,stroke-width:2px,rx:8,ry:8;
    classDef travel fill:#0e7490,stroke:#67e8f9,color:#ecfeff,stroke-width:2px,rx:8,ry:8;
    classDef property fill:#7e22ce,stroke:#e9d5ff,color:#faf5ff,stroke-width:2px,rx:8,ry:8;
```

Real routes in `App.tsx`: `/` (entry gate wraps everything), `/travel`, `/travel/trips/:tripId`, `/property`, `/property/cities/:cityId`, `/property/cities/:cityId/areas/:areaId`, `/property/cities/:cityId/areas/:areaId/localities/:localityId`, and `/settings` (§20). Travel/Property content is illustrative/static (see [§15](#15-functionality-chapters)) with no backend call behind it. `/settings` is the one exception — since 2026-09-16 it makes real HTTP calls to `travel-service`/`property-service` for currency options and master-refresh status/triggers (§20), even though that backend can't currently be exercised end-to-end from the machine this was built on (§12).

### 3.2 Navigation model

| Level | What | Example |
|---|---|---|
| **Primary** | Top navigation bar, always visible, with icons | Dashboard · Travel · Property |
| **Secondary** | Double-click a folder card to drill in (single click selects) | Travel folder → a trip folder → its detail page |
| **Contextual** | Tabs/pickers within a detail page | Route-comparison tabs, BHK-size picker, sort-by-rent toggle |

The folder metaphor from [docs/product-concept-ui.md](docs/product-concept-ui.md) was resolved as **stylized, not literal** (double-click semantics and folder visuals, plain CSS/React — not a simulated desktop window system).

### 3.3 Content taxonomy

The vocabulary each context uses to classify its own content — already defined in [docs/domain-model.md](docs/domain-model.md); listed here as the taxonomy, not redefined:

| Context | Content types |
|---|---|
| **Travel** | `TripPlan` (root) · `Destination` · `ItineraryDay` · `TravelLeg` (+ `TransportMode`) · `BorderCrossing` · `Accommodation` · `Budget` / `ExpenseItem` · `DocumentRequirement` · `PriceQuote` / `PriceSnapshot` · `AiSuggestion` |
| **Property** | `PropertyPlan` (root) · `CityProfile` · `AccommodationOption` · `MonthlyLivingCost` · `CommuteOption` · `CityScore` · `RentSnapshot` / `PropertyPriceSnapshot` · `AiRecommendation` |

Cross-cutting tags that apply across both: verification status (verified/unverified), AI status (`DRAFT`/`ACCEPTED`/`REJECTED`), and transport mode (air/train/road/waterway) for Travel legs specifically.

The frontend currently has its own smaller, illustrative version of this vocabulary, static in `tripData.ts`/`cityData.ts`/`areaData.ts` — `Trip`/`RouteOption`/`RouteLeg`/`LegOption` for Travel, `City`/`Area`/`Locality`/`BhkRent` for Property. These aren't the backend's domain types (no persistence, no API yet) but the shape is deliberately close, so wiring them to real endpoints later is a data-source swap, not a redesign.

### 3.4 Content hierarchy

What's prioritized on a page, top to bottom, as actually built:

- **Dashboard:** hero statement → "Last checked" recap (last-opened trip/city, if any). Deliberately just this — see [§14](#14-looking-ahead) and the decision log for why it's pinned to one viewport height with nothing else.
- **Trip detail:** cover image + title → route comparison (sort by cheapest/fastest, per-leg mode picker, running total) → journey map (illustrated waypoint trail) → itinerary (optional, hidden by default).
- **City detail:** cover image + title → areas list (sortable by rent) → "Did you know" facts specific to that city.
- **Locality detail:** name/area/pincode → BHK-size picker → add-on services (toggleable) → estimated total monthly cost.

---

## 4. Key terms

| Term | Plain-English definition | Why it matters here |
|---|---|---|
| **Monorepo** | One Git repository holding several apps instead of one repo each. | Review related changes together. [ADR-001](docs/decisions/ADR-001-monorepo.md). |
| **Bounded context** | A self-contained business area with its own rules — Travel and Property each define "plan" differently. From **Domain-Driven Design**. | Keeps Travel and Property from tangling as the project grows. |
| **Microservice** | A small, independently deployable app owning one bounded context. | `travel-service` and `property-service` own Travel's and Property's data; `integration-service` is the third, a bounded context of its own kind — not a business domain, but "talking to the outside world," owning zero data. [ADR-002](docs/decisions/ADR-002-microservices.md). |
| **SPA** | A web app that loads once, then updates itself without full page reloads. | `lifestyle-web` is a React SPA. |
| **Hexagonal architecture** (Ports & Adapters) | Core business logic (**domain**) knows nothing about frameworks or databases. "Ports" are interfaces it needs; "adapters" plug in the real tech (REST, database, AI). | Why `domain` packages have zero Spring/database imports — keeps rules testable and swappable. |
| **DTO** | A plain object that moves data across a boundary (API ↔ browser), kept separate from the domain model. | Internal changes don't silently break the public API shape. |
| **Schema (PostgreSQL)** | An isolated group of tables inside one database. | Travel and Property each own a schema; neither can touch the other's data. [ADR-003](docs/decisions/ADR-003-single-postgres-separate-schemas.md). |
| **Flyway migration** | A version-controlled SQL script, applied in order, tracked automatically. | Every environment's schema ends up identical. |
| **ADR** | A short, permanent record of one significant decision and its reasoning. | [docs/decisions/](docs/decisions/). |
| **JSONB** | A PostgreSQL column type for flexible, semi-structured data. | Only for variable provider metadata — never a substitute for typed columns. [docs/data-strategy.md](docs/data-strategy.md). |
| **Append-only snapshot** | A record that's never edited, only added to. | Price history stays comparable over time. [ADR-005](docs/decisions/ADR-005-price-snapshot-strategy.md). |
| **Provenance** | Metadata on where data came from — source, timestamp, confidence, verification. | Lets the system and user judge trustworthiness. |
| **LLM** | An AI model that reads/generates natural language (Llama, Gemma, Qwen). | Drafts suggestions only — never calculates facts. |
| **Ollama** | Free software that runs open-weight LLMs locally, no cloud account. | The project's default, fully optional AI provider. |
| **RAG** | Giving an AI model real retrieved data to reference before answering, reducing invented answers. | Planned later, backed by PostgreSQL full-text search first. |
| **Deterministic** | Same input always produces the same output, no randomness. | Money math, totals, sorting — always plain Java, never AI. |
| **C4 model** | Draws architecture at four zoom levels — Context, Container, Component, Code — one diagram per level. | [§5](#5-system-overview) is Context-level; [§6](#6-inside-one-backend-service) is Component-level. |

---

## 5. System overview

**Updated 2026-09-16 — now four apps, not three, and AI/external calls route through the fourth one, not straight from Travel/Property.** No cloud account, no paid API required — Groq/Mistral are free-tier cloud calls, everything else runs locally. (C4 Context level.)

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'stepAfter'}}}%%
flowchart TB
    User(("🧑 Person")) --> Web["lifestyle-web<br/>:5173"]

    Web -->|"/api/travel/**"| TravelSvc
    Web -->|"/api/property/**"| PropSvc

    subgraph Backend["Spring Boot services"]
        direction LR
        TravelSvc["travel-service<br/>:8081"]
        PropSvc["property-service<br/>:8082"]
        IntSvc["integration-service<br/>:8083"]
    end

    TravelSvc --> TravelSchema
    PropSvc --> PropSchema
    TravelSvc -->|"master refresh — live"| IntSvc
    PropSvc -->|"master refresh — live"| IntSvc
    TravelSvc -.->|"AI fallback — not yet wired"| IntSvc
    PropSvc -.->|"AI fallback — not yet wired"| IntSvc
    IntSvc --> IntSchema
    IntSvc --> AI
    IntSvc -->|"live"| GeoFrank
    IntSvc -.->|"manual endpoint only"| DataGovIn

    subgraph Data["PostgreSQL 18 + AI + free external APIs"]
        direction LR
        TravelSchema[("travel<br/>schema")]
        PropSchema[("property<br/>schema")]
        IntSchema[("integration<br/>schema")]
        AI{{"Ollama :11434 →<br/>Groq → Mistral"}}
        GeoFrank(["GeoNames +<br/>Frankfurter"])
        DataGovIn(["data.gov.in"])
    end

    class Web neutral;
    class Backend,Data groupBorder;
    class TravelSvc,TravelSchema travel;
    class PropSvc,PropSchema property;
    class IntSvc,IntSchema,GeoFrank,DataGovIn integration;
    class AI ai;

    classDef neutral fill:#334155,stroke:#94a3b8,color:#f1f5f9,stroke-width:2px,rx:8,ry:8;
    classDef groupBorder fill:transparent,stroke:#64748b,color:#e2e8f0,stroke-width:1.5px,rx:12,ry:12;
    classDef travel fill:#0e7490,stroke:#67e8f9,color:#ecfeff,stroke-width:2px,rx:8,ry:8;
    classDef property fill:#7e22ce,stroke:#e9d5ff,color:#faf5ff,stroke-width:2px,rx:8,ry:8;
    classDef integration fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,rx:8,ry:8;
    classDef ai fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,stroke-dasharray:4 4,rx:8,ry:8;
```

Solid arrows: real, live, actually called today. Dashed: designed but not yet wired, or manual-trigger only. Colors: slate = shell/frontend, blue = Travel, purple = Property, amber = the integration layer (both `integration-service` itself and everything past it — AI providers and external APIs).

The web app never touches the database directly — only through a service's REST API. Travel never reads Property's schema, and neither ever calls Groq/Mistral/GeoNames/Frankfurter/data.gov.in directly — only `integration-service` does. [§7](#7-data-architecture), [§18](#18-ais-role-in-this-application).

---

## 6. Inside one backend service

All three services are structured identically, using hexagonal architecture ([§4](#4-key-terms)) — including `integration-service`, even though its "domain" is external calls rather than business rules. This is the most important structural rule in the codebase. (C4 Component level, one zoom step in from [§5](#5-system-overview).)

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'stepAfter'}}}%%
flowchart LR
    Client(("HTTP request")) --> Controller["REST Controller"]
    Controller --> UseCase

    subgraph Core["domain + application — framework-free"]
        direction TB
        UseCase{{"use case / port"}} --> Domain["domain rules"]
    end

    UseCase --> Repo["Repository adapter"]
    Repo --> Postgres[("PostgreSQL<br/>this schema only")]

    class Core groupBorder;
    class Controller,Repo inout;
    class UseCase,Domain core;
    class Postgres data;

    classDef groupBorder fill:transparent,stroke:#a5b4fc,color:#e0e7ff,stroke-width:1.5px,rx:12,ry:12;
    classDef inout fill:#0e7490,stroke:#67e8f9,color:#ecfeff,stroke-width:2px,rx:8,ry:8;
    classDef core fill:#4338ca,stroke:#c7d2fe,color:#eef2ff,stroke-width:2px,rx:8,ry:8;
    classDef data fill:#15803d,stroke:#86efac,color:#dcfce7,stroke-width:2px,rx:8,ry:8;
```

`domain`/`application` (center) hold the business rules in plain Java — no Spring, no JPA, no HTTP. The adapters either side are replaceable plugs: swap the REST controller for a CLI, or PostgreSQL for something else, and the business rules never change. `config` (not pictured — pure Spring wiring) connects adapters to ports.

This also makes AI safe by design ([§8](#8-ai-strategy)): it's just another outbound adapter. If it's off or fails, the use case still runs — it simply has no AI suggestion available.

---

## 7. Data architecture

One PostgreSQL 18 server for local development; three contexts (`travel`, `property`, `integration`) now share it as of 2026-09-16, but never share data — no cross-schema queries, each with its own role and Flyway history.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'stepAfter'}}}%%
flowchart TB
    TravelSvc["travel-service"] -->|"owns"| TS[("travel schema<br/>role: travel_app")]
    PropSvc["property-service"] -->|"owns"| PS[("property schema<br/>role: property_app")]
    TravelSvc -.->|"never queries"| PS
    PropSvc -.->|"never queries"| TS

    class TravelSvc,TS travel;
    class PropSvc,PS property;

    classDef travel fill:#0e7490,stroke:#67e8f9,color:#ecfeff,stroke-width:2px,rx:8,ry:8;
    classDef property fill:#7e22ce,stroke:#e9d5ff,color:#faf5ff,stroke-width:2px,rx:8,ry:8;
```

One PostgreSQL 18 instance, three schemas (`travel`/`property` above, plus `integration` — narrower, holding only `ai_suggestion` for AI-draft provenance, not business data), each its own login role and Flyway history — a setup convenience, not shared ownership. No cross-schema foreign keys, no shared queries; cross-context needs go through APIs. (This is an ownership diagram, not a true ER diagram — for that, see [§16](#16-planned-backend-data-model--masters-transactions-audit)'s real, flat-entity ER diagrams, which describe tables that now genuinely exist via Flyway, not a future-tense plan.)

- **Typed columns for facts, JSONB only for variable extras** — amount, currency, source, timestamp are always typed and queryable.
- **Price history is append-only** — corrections add a new observation, never overwrite the old one.

---

## 8. AI strategy

**Update 2026-09-16: this shape is no longer just planned — it's built, in `integration-service`.** The diagram below was written before any of it existed; it's kept because the shape it describes turned out to match what actually got built almost exactly (Ollama as the local/default provider, cloud providers as opt-in fallback, DRAFT→ACCEPTED lifecycle). See [§18](#18-ais-role-in-this-application) for the real, working detail: `AiProviderRouter` (Ollama → Groq → Mistral), real persistence of every draft into `integration.ai_suggestion`, and exactly what's still not wired up (the AI fallback isn't yet called by `travel-service`/`property-service`'s search). [ADR-004](docs/decisions/ADR-004-ai-provider-abstraction.md) has the original reasoning.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'basis'}}}%%
flowchart TB
    subgraph Core["application layer"]
        UseCase["Use case"] --> Port{{"AiProvider<br/>port"}}
    end

    Port -.-> Ollama["Ollama adapter<br/>local, default"]
    Port -.-> Paid["Paid adapter<br/>opt-in only"]
    Port --> Fallback(["No provider?<br/>manual still works"])
    Ollama --> Draft["AiSuggestion<br/>DRAFT"] -->|"human<br/>approves"| Accepted(["Accepted —<br/>now authoritative"])

    class Core groupBorder;
    class UseCase,Port core;
    class Ollama ai;
    class Paid paidai;
    class Draft,Accepted,Fallback result;

    classDef groupBorder fill:transparent,stroke:#c7d2fe,color:#eef2ff,stroke-width:1.5px,rx:12,ry:12;
    classDef core fill:#4338ca,stroke:#c7d2fe,color:#eef2ff,stroke-width:2px,rx:8,ry:8;
    classDef ai fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,stroke-dasharray:4 4,rx:8,ry:8;
    classDef paidai fill:#be123c,stroke:#fecdd3,color:#fff1f2,stroke-width:2px,stroke-dasharray:4 4,rx:8,ry:8;
    classDef result fill:#15803d,stroke:#86efac,color:#dcfce7,stroke-width:2px,rx:8,ry:8;
```

1. **AI never calculates money.** Totals, scores, sorting: deterministic Java only. AI explains or drafts, never computes.
2. **Every suggestion starts as a draft**, with model/prompt/timestamp/source recorded, until a human accepts it.
3. **The app works with AI off.** Non-AI workflows never depend on Ollama running.

Rollout order: structured extraction → missing-field detection → tool calling → explanations → RAG. [docs/ai-capability-roadmap.md](docs/ai-capability-roadmap.md).

---

## 9. Technology stack

Every entry is stable, free, and runs locally — no paid service required. Exact pinned versions: [docs/technology-versions.md](docs/technology-versions.md).

**Frontend**

| Technology | What it is | Why |
|---|---|---|
| React | UI library, reusable components | Dominant, well-documented SPA choice |
| TypeScript | JavaScript with static types | Catches mistakes before runtime |
| Vite | Build tool + dev server | Fast reload; provides the `/api/**` dev proxy |
| React Router | Client-side page routing | Already used in the app shell and the Travel folder/trip routes |
| TanStack Query | Server-state fetching/caching | Installed and the provider is wired at the app root (`main.tsx`), but not yet actually used anywhere — no `useQuery` call exists in the codebase yet. The Settings page's real backend calls (§20) use a plain `fetch` wrapper (`lib/apiClient.ts`) instead, for now |
| Vitest | Test runner | Fast feedback, native Vite/TS support |
| Tailwind CSS | Utility-class CSS framework (classes composed directly in JSX instead of hand-written stylesheet rules) | Enterprise-standard way to style a React app without a growing hand-rolled CSS file; `styles.css` is now just Tailwind's import plus the brand's design tokens (colors, fonts) as `@theme` variables — custom CSS only for the brand identity, nothing else |
| Heroicons | Icon set from the Tailwind team (outline/solid SVG components) | One consistent, minimal icon language across nav/folder cards, instead of mixed emoji |
| canvas-confetti | Small, dependency-free confetti-burst animation | The entry gate's celebratory moment on "Enter" — tiny (a few kB), no other library pulled in for it |

**Backend** (`travel-service`, `property-service`, `integration-service` — see [§18](#18-ais-role-in-this-application) for why there are three, not two)

| Technology | What it is | Why |
|---|---|---|
| Java (LTS) | Statically typed, long-term-supported language | Fewer forced upgrades; suits hexagonal domain modeling |
| Spring Boot | Web/DI framework | Industry standard for Java REST services |
| Gradle (wrapper checked in) | Build tool | Every clone gets the identical Gradle version |
| Flyway | DB migrations ([§4](#4-key-terms)) | Every environment's schema stays identical — real migrations now exist and have created the full §16 schema for travel/property, plus `integration.ai_suggestion` |
| Plain `JdbcTemplate`, not JPA | Direct SQL, no ORM | Deliberate for now — the only persistence code that exists (§12, §18, §20) is a handful of upsert/insert statements; no entity-mapping layer has been introduced yet |
| Resilience4j (core modules only) | Rate limiting + circuit breaking | Protects free-tier AI/API quotas (§17.3, §18) — the core library, constructed by hand, not the Spring Boot starter/autoconfig module, since that module's Boot-4.1 compatibility couldn't be verified on the machine that added it |
| springdoc-openapi (Swagger UI) | Generates a live API reference (`/swagger-ui.html`) + machine-readable contract (`/v3/api-docs`) from the controllers | Self-documenting API — can't silently go stale like hand-written docs |

**Data & infrastructure**

| Technology | What it is | Why |
|---|---|---|
| PostgreSQL | Free, ACID-compliant relational database | Typed columns + JSONB covers everything Phase 1–2 need; a real instance now exists with `travel`/`property`/`integration` schemas (§12) |
| Docker Compose | Multi-container local orchestration | **Reconciled 2026-09-17** — `compose.yaml` now containerizes all four apps (`lifestyle-web`, `travel-service`, `property-service`, `integration-service`, each with its own `Dockerfile`), plus `postgres`/`ollama`, with Compose Watch (`develop.watch`) rebuilding a service on source changes. The earlier native-Postgres note is resolved — this Compose file is the real setup now, run and verified working on the machine that has a real JDK 25 |
| Ollama | Local LLM runner, no cloud account | No longer just optional-in-principle — it's `integration-service`'s **first-priority** AI provider (§18), ahead of Groq/Mistral, confirmed working with `qwen3:4b-instruct` on the machine that has it running |

**Explicitly deferred** (not installed): Spring AI (no call site yet), pgvector, PostGIS, Redis, Kafka, Elasticsearch, MinIO, any second database. Each has a documented trigger condition in [docs/data-strategy.md](docs/data-strategy.md) — none added speculatively.

---

## 10. Non-negotiable engineering rules

- **Bounded contexts stay separate** — no shared tables, cross-schema FKs, or shared domain types. APIs only.
- **Domain code stays framework-free** — no Spring/JPA imports in `domain`. [§6](#6-inside-one-backend-service).
- **No paid services, auth, or new infra without explicit approval.**
- **AI output is never authoritative** — prices, visas, totals come from a verified source or deterministic code.
- **Money math is deterministic** — plain Java, reproducible.
- **Services are stateless** — no server-side session state; every request self-contained. Lets a service scale to N instances with zero rework; the only state that matters lives in PostgreSQL.
- **Generic over bespoke, but only while it stays simple** — reuse an existing component's shape by changing data/config before writing a new one; go bespoke only when genericizing would make the shared version *more* complex than two simple, separate things.

Full context: [AGENTS.md](AGENTS.md).

---

## 11. Architecture Decision Records

| ADR | Decision |
|---|---|
| [ADR-001](docs/decisions/ADR-001-monorepo.md) | One monorepo; apps still build/run independently |
| [ADR-002](docs/decisions/ADR-002-microservices.md) | Two independent services, not one combined app |
| [ADR-003](docs/decisions/ADR-003-single-postgres-separate-schemas.md) | One PostgreSQL instance, isolated schemas per service |
| [ADR-004](docs/decisions/ADR-004-ai-provider-abstraction.md) | AI behind a replaceable port; must run without it |
| [ADR-005](docs/decisions/ADR-005-price-snapshot-strategy.md) | Prices stored as immutable, append-only snapshots |

---

## 12. Where the project is today

Two different things are true at once here, and it's worth being precise about which is which:

- **Backend (`travel-service`, `property-service`, `integration-service`):** more built than "Phase 1 scaffolding" implies, but with real gaps that matter. A real Postgres instance now exists (on the Java-25-capable machine, not this one) with `travel`/`property`/`integration` schemas and restricted per-service roles. Flyway has created the **entire planned schema** from §16 for Travel and Property — masters, transaction tables, and audit log, not just a marker table — plus `integration.ai_suggestion` for AI-draft provenance. No JPA entity layer exists anywhere (a deliberate choice, not an oversight — see the decision log): the only code actually touching these tables is plain `JdbcTemplate`, and it only touches a few of them: `city`/`currency` masters (via `MasterRefreshUseCase`, §20) and `ai_suggestion` (via `PlaceSuggestionUseCase`, §18). Everything else in the schema — `trip_plan`, `route_option`, `rent_snapshot`, `cost_estimate`, and the rest — is real, created DDL with **zero application code reading or writing it yet**. Place *search* (`PlaceSearchController`) still reads from `InMemoryPlaceRepository`, a fixture list, **not** the now-real `city` table `MasterRefreshUseCase` populates — an inconsistency worth closing next, not a hidden secret. Cross-service HTTP calls exist for master refresh (`travel`/`property` → `integration-service` → GeoNames/Frankfurter) but not yet for the AI search fallback. Everything built on the Java-8 office-laptop machine this session (the master-refresh feature, the third microservice) is **written but not compiled** there — see §18/§20 for exactly which files and why that gap exists.
- **Frontend (`lifestyle-web`):** has grown well past a "shell" — a real entry experience, a working Travel flow (trip folders → route comparison with sortable multi-mode legs → an illustrated journey map → optional itinerary), a working Property flow (city folders → area/locality drill-down → BHK-size + add-on cost estimator), a working Settings page (§20), and a Dashboard AI-status widget (§15.4) all exist and run. Travel/Property content is still backed by static illustrative data (see [§3.3](#33-content-taxonomy)), not a live API — Settings and the Dashboard widget are the **two exceptions**: the only parts of this app that make a real HTTP call to a backend, though that backend can't currently be exercised end-to-end from this same machine (see above).

So: don't read "Phase 1" as "not much is built" anymore — real schema, real (if narrow) persistence, and a real cross-service call all exist now. Still genuinely not built anywhere: live price providers, a scheduler, most of the planned schema actually being read/written by application code, PDF export, auth, hosted deployment. [docs/roadmap.md](docs/roadmap.md) · [README.md](README.md).

### Responsibility map — what's responsible for what

One line per component, answering exactly one question each — useful when you're trying to find *which file* owns a piece of behavior.

| Component | Responsible for | Not responsible for |
|---|---|---|
| `lifestyle-web` | Everything you see and click; two real backend calls exist (Settings, Dashboard AI status) | Never talks to Postgres directly; never calls Groq/Mistral/GeoNames itself |
| `travel-service` | Travel's own data (schema `travel`), its `PlaceSearchUseCase` (search fixture, not the real `city` table yet), its `MasterRefreshUseCase` (city+currency, real) | Property's data; talking to Groq/Mistral/GeoNames/Frankfurter directly — always via `integration-service` |
| `property-service` | Property's own data (schema `property`), the same search/refresh pair as Travel, scoped to Property's masters | Travel's data; same external-call restriction as above |
| `integration-service` | Every external call, from any service — AI providers (`AiProviderRouter`), GeoNames, Frankfurter, data.gov.in; rate limiting, circuit breaking, and health tracking for all of them (`ResilienceGuard`); the one small `ai_suggestion` table for AI-draft provenance | Any Travel/Property business data; it owns no `trip_plan`, no `property_plan` |
| `ResilienceGuard` (inside integration-service) | Rate limiting + circuit breaking + health recording, for every outbound call, uniformly | Deciding *whether* to call — that's each `AiProvider`/client's `isConfigured()` check, done before `ResilienceGuard` is ever reached |
| `HealthStatusRegistry` (inside integration-service) | Remembering the outcome of the last real call per provider/client | Making any call itself — it's a passive record, written to only by `ResilienceGuard` |
| PostgreSQL | Storing whatever a use case actually chooses to persist — masters, AI drafts | Deciding what gets stored — that's each service's own use-case layer, in Java, never inferred from the schema |

### Data flow — what actually happens when you use the UI, and what (if anything) gets stored

This is worth being blunt about, since it's easy to assume more is wired up than actually is. Table below: an action you can take in the UI today, what happens when you do it, and whether anything reaches a database.

| You do this in the UI | What actually happens | Does it write to a database? |
|---|---|---|
| Browse Travel (trip folders, route comparison, journey map) | Reads `tripData.ts` — a static TypeScript array bundled into the app | **No.** Nothing is read from or written to Postgres. This is illustrative data, not your data. |
| Browse Property (city → area → locality, cost estimator) | Reads `cityData.ts`/`areaData.ts` — same static-array pattern | **No.** Same as above. |
| Type into a Travel/Property search box | `PlaceAutocomplete` calls `placeSearch.ts`, which searches the *same* static arrays client-side — no network call at all | **No.** This doesn't even reach a backend; it's pure frontend logic. |
| Open Settings, view currency options / master status | Real `fetch` calls to `travel-service`/`property-service`'s `/masters/currencies` and `/masters/refresh-status` | **Read-only** — reports what's already in `travel.currency`/`property.currency` and `*.master_refresh_log`. Nothing is written by viewing. |
| Click "Refresh master data" in Settings | `POST /api/{travel|property}/v1/masters/refresh` → `MasterRefreshUseCase` → `integration-service` (GeoNames + Frankfurter) → upserts into `travel.city`/`travel.currency` (or `property.*`) | **Yes — the only UI action in this app that writes real data today.** Upsert-only (§17.1), plus one row appended to `master_refresh_log` per master, every attempt, even on failure. |
| View the Dashboard's AI-status dots | Real `fetch` to `integration-service`'s `/ai/providers/status`, reading `HealthStatusRegistry` | **No write** — this is a read of in-memory state, not the database, and it doesn't call Ollama/Groq/Mistral either. |
| (Not yet exposed in any UI) An AI place-suggestion request | `PlaceSuggestionUseCase` calls a real AI provider, then writes the draft | **Yes**, into `integration.ai_suggestion` — but nothing in the UI can trigger this yet; it's only reachable by calling `integration-service`'s endpoint directly. |

The short version: **almost everything you can click today is a read of static frontend data, not a database.** The one exception that both writes to a real table *and* is reachable from the UI is the Settings page's "Refresh master data" button. Everything else described in §16 (trips, property plans, rent history, cost estimates) has real Postgres tables sitting empty, waiting for the CRUD backend that hasn't been built yet (phased plan, item 2).

---

## 13. Decision log

Notable calls, newest first — not routine changes. Append a new entry; never rewrite or delete a past one (a reversal gets its own new entry, linked back).

- **2026-09-17 — Confirmed the Docker networking fix worked, then found and fixed a second, real bug: `integration-service`'s credentials were never actually passed into its container.** Live evidence (Frankfurter returning real EUR/USD rates, confirmed in `travel.currency` via the API) proved `INTEGRATION_SERVICE_URL` routing is genuinely fixed. Two remaining gaps, both now fixed: (1) `compose.yaml`'s `integration-service` block never listed `GEONAMES_USERNAME`/`GROQ_API_KEY`/`MISTRAL_API_KEY`/`DATA_GOV_IN_API_KEY` at all — Compose does not forward host/`.env` variables into a container unless a service's own `environment:` block explicitly names them, so these were always unset inside the container regardless of what was in `.env`; added all four (plus their optional model/resource-id overrides) with empty defaults, matching the existing "unset = unconfigured, skip gracefully" pattern. (2) `AED` (in the default tracked-currency list) was live-confirmed **not supported by Frankfurter** — it's ECB-reference-rate-based, and AED isn't part of ECB's published currency set (`GET /v1/currencies` has no AED key) — swapped for `GBP` in both services' `application.yaml` defaults, an honest fix rather than silently letting that one tracked currency fail every run forever. Also added real per-attempt logging inside `integration-service` itself, which had none before this — `ResilienceGuard` now logs INFO/WARN with name, duration, and outcome for every guarded call (success, rate-limited, circuit-open, or failed), and `GeoNamesClient`/`DataGovInClient` log an explicit "skipped — not configured" line when their credential is absent, addressing a real gap: previously only the *caller* side (`travel-service`/`property-service`'s `IntegrationServiceClient`) logged anything, so `integration-service`'s own console showed nothing for a skipped or successful call. *Why:* live diagnostics from real running containers (not guesses) surfaced two genuinely separate root causes hiding behind the same symptom — a credential-passthrough gap and a real free-tier API coverage limit — and a console with no success/skip visibility made every future diagnosis this hard again if left unfixed.

- **2026-09-17 — Diagnosed the Docker networking bug down to root cause (confirmed via real container logs), fixed `compose.yaml`, and expanded master refresh with three network-independent masters while the container-recreation deploy step was still pending.** Real logs from `travel-service`/`property-service` (`ResourceAccessException: I/O error ... baseUrl=http://localhost:8083`) confirmed exactly the hypothesis from the previous entry: `INTEGRATION_SERVICE_URL` was never actually applied to the running containers, even after the `compose.yaml` fix landed on disk — most likely because a long-running `docker compose watch` process, started before that edit, only reacts to source-code changes in its `develop.watch` paths and never re-reads `compose.yaml` itself; recreating requires an explicit `docker compose up -d`. Multiple diagnostic-only reports (confirming the same root cause without applying or verifying the fix) made this take longer to resolve than it should have — the lesson generalized into an explicit instruction to whichever tool applies fixes going forward: paste literal command output, don't summarize "done." Separately, added real, working master refresh for `transport_mode` (travel) and `bhk_type`/`service_addon` (property) — a genuinely different kind of master from `city`/`currency`: fixed categorical label sets with no external source, seeded directly by `MasterRefreshUseCase` with zero network dependency, so they succeed regardless of the `integration-service` connectivity bug. Deliberately did not seed `visa_requirement` (would mean inventing unverified facts, violating §10) or `municipality` (needs a `city_id` FK lookup dependency not yet built). The Settings page needed no frontend changes to display the new masters, since it already renders `GET /masters/refresh-status` generically. *Why:* the user asked for "all masters" on one refresh, and pointed out that repeated diagnosis without a confirmed applied fix wasn't converging — this both closes the gap where genuinely possible right now (categorical-label masters) and is explicit about which masters remain out of scope and why, rather than papering over the ones still blocked on the network fix or an unverified external schema.

- **2026-09-17 — Diagnosed and fixed a real bug: master-data refresh returned only the base currency and zero cities, root-caused to a Docker networking default, not a code defect in the refresh logic itself.** The user reported "only 1 INR came from currency, no cities came." Traced it: `MasterRefreshUseCase.refreshCurrencies()` always upserts the base currency locally regardless of whether the network call succeeded, which is why INR appeared even when everything else failed; `IntegrationServiceClient.findCity()`/`fetchExchangeRates()` (in both `travel-service` and `property-service`) were silently swallowing every exception with no logging, so the real cause was invisible. Root cause, confirmed once `compose.yaml` finished syncing over from the machine actually running this: `travel-service`/`property-service` had no `INTEGRATION_SERVICE_URL` set in their container environment, so both fell back to `application.yaml`'s default (`http://localhost:8083`) — inside a container, `localhost` means "this container itself," not the `integration-service` container, so every master-refresh call failed before it ever left the container. Fixed by adding `INTEGRATION_SERVICE_URL: http://integration-service:8083` to both services' `compose.yaml` environment blocks. Also fixed the invisibility problem itself, independent of this specific bug: added SLF4J WARN logging to both `IntegrationServiceClient`s (logs the target URL and real exception on any failure) and reworded the "seed cities not found" outcome message to stop asserting a specific cause ("not found in GeoNames") when the actual failure mode couldn't be distinguished from "couldn't reach integration-service at all." *Why:* a bug report with no server-side visibility into *why* a call failed is very hard to diagnose remotely — fixing the silent-swallowing matters as much as fixing this one instance, since the next failure (different cause) would otherwise be equally invisible.

- **2026-09-16 — Researched every external API's real rate limit, closed a rate-limiting gap that existed on three of them, and added real "last-hit" health tracking.** Researched (web search, not assumed) and confirmed: GeoNames 1,000 credits/hour; Groq 30 req/min org-wide; Mistral 1 req/**second** org-wide (not per-minute — a per-minute bucket would let a burst blow past this); Frankfurter publishes no hard quota; **data.gov.in publishes no numeric limit anywhere found**, flagged as explicitly unverified rather than guessed with false confidence. Discovered while researching this that `GeoNamesClient`/`FrankfurterClient`/`DataGovInClient` had **zero rate limiting** — only the three AI providers did. Fixed by extracting a new shared `ResilienceGuard` (`adapter/out/resilience`) that wraps any named call with a Resilience4j `RateLimiter` + `CircuitBreaker`, replacing `AiProviderRouter`'s previously bespoke, hand-rolled maps, and applying it uniformly to all six outbound calls (3 AI providers + GeoNames + Frankfurter + data.gov.in) — each with its own config sourced from the table above, expressed as `limitForPeriod`/`refreshPeriodMs` rather than a single "per minute" number specifically so Mistral's sub-second cap can be represented correctly. Added `HealthStatusRegistry`, updated as a side effect of every `ResilienceGuard` call — health is decided by the outcome of the last real call and updated on the next one, never a synthetic ping, per explicit instruction. `GET /api/integration/v1/ai/providers/status` and the new `GET /api/integration/v1/masters/health` now report this real health alongside the existing `configured` flag. Also added `AiProviderRouter.suggestPlacesVerified()` — calls Ollama+Groq in parallel and reports agreement, built exactly per §18's own prior "does more AI mean more accuracy" research, but deliberately **not** called by the default `PlaceSuggestionUseCase` path, since sequential fallback is the only mode the real rate limits above actually support for routine calls. Built the Dashboard's AI-status widget (`features/dashboard/AiStatusWidget.tsx`) — green/red/orange dots, reading the enriched status endpoint, rendering nothing (not an error) if `integration-service` is unreachable. Rewrote §12's status summary, §18's multi-provider section, and the Spring-AI subsection (added an explicit "we don't use it — here's the real code instead" correction, since that had drifted from a recommendation into an implied fact) to match. Added two new subsections under §12 — a responsibility map and a data-flow table stating plainly that almost every UI action today reads static frontend data, and the Settings refresh button is the only one that writes to a real table. Frontend verified (`npm run lint`/`test`/`build` all pass); backend written but **not compiled** — same Java-25-toolchain gap as everything else this session, reconfirmed clean on `integration-service` after this pass. *Why:* explicit ask — verify real limits so free tiers never get blocked, decide health from real traffic, decide sequential-vs-parallel for accuracy, and be honest about what's actually stored where.

- **2026-09-16 — Built the Settings page (§20) and a real, scoped master-data refresh, against the real Postgres schema a parallel session had already stood up.** Read the actual `V2__domain_tables.sql` in `travel-service`/`property-service` first rather than guessing column names, since `spring.jpa.hibernate.ddl-auto: validate` (enabled by the other session) means any mismatch would fail loudly at startup. Added one additive migration per service (`V3__master_refresh_log.sql`: a `master_refresh_log` table plus a natural-key `UNIQUE (name, country_code)` constraint on `city`, needed for `INSERT ... ON CONFLICT` upserts — `currency.iso_code` already had one) rather than editing the already-applied V1/V2. Built `MasterRefreshUseCase` in both domain services, using plain `JdbcTemplate` (not JPA — no entities exist anywhere in this codebase yet, and this is a handful of upsert statements, not a case needing an ORM), calling a new `IntegrationServiceClient` that relays every external call through `integration-service` rather than reaching GeoNames/Frankfurter directly, matching the third-microservice design from the previous session. Added `FrankfurterClient` to `integration-service` (genuinely free, no key) as the currency-rate source. Frontend: `SettingsPage.tsx` renders currency options and master-refresh status entirely from what the backend returns — verified via `npm run lint`/`test`/`build`, all passing, plus a dev-server smoke check that `/settings` serves. Deliberately **did not** attempt property's `area`/`locality`/`pincode` refresh via data.gov.in (its response schema has never been inspected against a real key, and a wrong guess could silently corrupt the location hierarchy with no way to detect it), and did not fabricate a "transactions refresh" since no transaction-creation UI/backend exists yet for anything to pull. *Why:* asked for a Settings tab where "first refresh" pulls master data and nothing in the UI is hardcoded — delivering that honestly meant scoping to the two masters with a genuine, safe, already-researched external source (city via GeoNames, currency via Frankfurter) rather than half-building five masters, three of which have no real refresh source at all.

- **2026-09-16 — Extracted a third microservice, `integration-service`, to hold every external API call; reverted the same code out of `travel-service`/`property-service`.** The AI-provider (Groq/Mistral) and master-data (GeoNames/data.gov.in) code added in the immediately preceding entry had been written directly inside both domain services — on reflection, a reasonable but avoidable duplication, since neither service's job is "talk to Groq," and both would otherwise carry a second, identical copy of every provider client and Resilience4j wrapper. Deleted `adapter/out/ai`, `adapter/out/geonames`, `adapter/out/datagovin`, `ExternalApisProperties`, `AiProvider`, `AiProviderStatusController`, and `MasterLookupController` from both services; reverted `PlaceKind`, `PlaceSearchUseCase`, `PlaceSearchUseCaseTest`, each `*Application.java`, `build.gradle` and `application.yaml` back to their state before that entry — confirmed via `find` that both services' file trees now match exactly. Built `integration-service` from scratch at the repo root as a genuine third microservice, following the same conventions as the other two exactly: own `settings.gradle`/`build.gradle`/Gradle wrapper (copied, not reinvented), same Spring Boot 4.1.1 + Java 25 toolchain, same `domain`/`application`/`adapter.in.web`/`adapter.out.*`/`config` hexagonal layering, same `ServiceInfoController`/`OpenApiConfig` pattern, port `8083` (`INTEGRATION_PORT`), base path `/api/integration/v1`. It deliberately omits `adapter.out.persistence` and the JPA/Flyway/Postgres/H2 dependencies the other two carry — it owns no bounded-context schema, so those would be dead weight, not consistency. Moved `AiProviderRouter`/`GroqAiProvider`/`MistralAiProvider`/`GeoNamesClient`/`DataGovInClient` into it as-is, added a new `PlaceSuggestionUseCase` + `PlaceSuggestionController` (`GET /api/integration/v1/ai/place-suggestions?q=`) as its application-layer entry point, plus `AiProviderStatusController` and `MasterLookupController`, and one JUnit 5 test class. `README.md`, `scripts/start-local.sh`, and `docs/agent-context/CURRENT-STATE.md` updated to list the third service alongside the other two. **`travel-service`/`property-service` do not yet call `integration-service` over HTTP** — that cross-service wiring is designed (§18 diagram) but not built, an honest gap, not an oversight. Reconfirmed `./gradlew compileJava --offline` fails identically (missing JDK 25 toolchain only, no dependency-resolution errors) on all three services after this restructuring. *Why:* explicit ask — "create a third microservice... shift all the external APIs calling logic... in that particular microservice," on the reasoning that a dedicated integration layer is better architecture than duplicating outbound-call code into two domain services that shouldn't need to know how to talk to Groq.
- **2026-09-16 — Implemented all four external API integrations from §22 as real, credential-driven code, written but not compiled on this machine.** Added to both services: `GroqAiProvider` and `MistralAiProvider` (`adapter/out/ai`), calling each provider's actual OpenAI-compatible chat-completions endpoint via Spring's `RestClient`, and `AiProviderRouter`, which tries them in priority order (Groq, then Mistral), skipping any with no API key configured and falling through to the next on failure — wrapped per-provider in a hand-constructed Resilience4j `RateLimiter` + `CircuitBreaker` from the framework-core modules (`resilience4j-ratelimiter`/`resilience4j-circuitbreaker`), not the Spring Boot starter/autoconfig module, whose Boot-4.1 compatibility couldn't be verified here. `PlaceSearchUseCase` (built in the previous pass) now calls this router as its real AI fallback when local search finds nothing — results come back tagged `AI_SUGGESTION` with an explicit unverified disclaimer, never merged with real matches. Also added `GeoNamesClient` (both services) and `DataGovInClient` (property-service), thin `RestClient` wrappers over the free APIs researched and account-registered in §22, each reading its credential from an environment variable (`GEONAMES_USERNAME`, `DATA_GOV_IN_API_KEY`, `GROQ_API_KEY`, `MISTRAL_API_KEY` — names only, never values, documented in §22's new table) and returning an empty result rather than throwing when unset. Neither is wired to a scheduled master-refresh job yet, since that needs real persistence this pass deliberately left out; each instead gets a manual-trigger debug endpoint (`MasterLookupController`) so the integration is genuinely callable and provable today. New `AiProviderStatusController` (`GET /api/{travel|property}/v1/ai/providers/status`) reports which providers are configured via a config check, not a live ping, so it costs no API quota. Added `ExternalApisProperties` (`@ConfigurationProperties(prefix = "app")`, records, bound via `@ConfigurationPropertiesScan` on each `*Application` class) as the single place `application.yaml`'s `${ENV_VAR}` placeholders resolve into. **Could not compile or run** — reconfirmed via `./gradlew compileJava --offline` on both services, which still fails only on the known missing-JDK-25-toolchain error, with no dependency-resolution errors for the two new Resilience4j coordinates; every file was instead hand-reviewed for syntax. *Why:* explicitly asked to implement every external API integration discussed, using whatever keys already exist, while still leaving the actual database out — reading credentials from environment variables and keeping every client's absence-of-key path harmless (empty result, not a crash) makes that possible without ever needing to see, store, or commit an actual secret value.
- **2026-09-16 — Built the backend half of the search/autocomplete feature (§18) in both services, written but not compiled on this machine.** Added to `travel-service` and `property-service`, following the existing `domain`/`application`/`adapter` layering: `PlaceSearchUseCase` (application), a `PlaceRepository` outbound port interface (`application/port/out`), an `InMemoryPlaceRepository` adapter (`adapter/out/persistence`) holding the same illustrative places as the frontend's `tripData.ts`/`cityData.ts`/`areaData.ts`, and a `PlaceSearchController` exposing `GET /api/{travel|property}/v1/places/search?q=` — same DB-first-then-honest-fallback-notice contract as `lifestyle-web`'s `placeSearch.ts`/`PlaceAutocomplete.tsx`, field-for-field. This is the first place in the backend where the port/adapter split (present in the skeleton only as an empty `adapter/out/persistence` package before now) is actually used for something: the in-memory adapter is explicitly a temporary placeholder for a real Postgres query, swappable later without touching the use case or controller. Also added one JUnit 5 test class per service (`PlaceSearchUseCaseTest`) covering the minimum-query-length guard, a known-place match, and the AI-fallback-notice path. **Could not compile or run this locally** — confirmed via `./gradlew compileJava --offline`, which fails cleanly asking for a JDK 25 toolchain (this machine has Java 8, plus an unrelated Java 17 install, neither sufficient) rather than attempting to download one, consistent with the standing rule against changing this machine's system configuration. `docs/agent-context/CURRENT-STATE.md` updated to record this as written-but-unverified. *Why:* explicitly asked to implement this in the backend too, not just the frontend, while leaving the actual database creation for the user's own machine — an in-memory adapter behind a port interface does exactly that: real, working backend code with no real database anywhere near it.
- **2026-09-16 — Built the frontend half of the search/autocomplete feature (§18): DB-first search over local data, honest AI-fallback notice, no fake AI call.** New `lib/placeSearch.ts` (`searchPlaces(scope, query)` — matches against `cityData`/`areaData`'s city/area/locality names, or the place strings already saved inside `tripData`'s trip legs; locality results carry their area/city as parent context so a repeated name like "Gandhi Nagar" is never ambiguous) and a generic `components/PlaceAutocomplete.tsx` (controlled input + dropdown), wired into `TravelFolderPage` (filters visible trip folders by a matched place) and `CityFolderPage` (navigates straight to the matched city/area/locality) — one component, two call sites, matching the design in §18 exactly. Deliberately **did not** fake an AI fallback: there is no backend and no AI provider connected in this codebase yet, so when local search finds nothing, the dropdown shows `AI_FALLBACK_NOTICE` — a plain, honest string explaining that this is where an AI suggestion would appear once wired up — rather than simulating a response that doesn't actually exist. Verified with `npm run lint`, `npm test`, `npm run build` (`tsc --noEmit` + Vite build), all passing. The real backend `PlaceSearchService`, GeoNames-backed hierarchy, and AI-drafted "via" suggestions remain design-only per the user's explicit instruction to leave the database for their own machine and implement only what's codeable now. *Why:* asked to start implementing whatever parts of the plan don't require the (not-yet-created, local-only) database — this was exactly that: the searchable "masters" already exist as this app's own placeholder data, so the search behavior itself could be built and proven now, with the AI/backend seams left honestly stubbed rather than faked.
- **2026-09-16 — Designed the smart search/autocomplete feature (§18) as database-first, AI-fallback-only.** One generic `PlaceSearchService` + `PlaceAutocomplete` component, reused by both Travel's from/to fields and Property's location search per the generic-over-bespoke rule — the only difference is which master tables get searched (Travel: `city`; Property: `city`/`area`/`locality`/`pincode`). Local masters and the user's own saved route history are searched first and returned instantly, free and instant; an AI call only happens as a fallback when nothing local matches at all, which also directly protects the free-tier rate limits already designed in §17.3 from being exhausted by ordinary typing. The India→state→city and duplicate-name ("Gandhi Road, Pune" vs. "Gandhi Road, Mumbai") cases are both plain deterministic queries/formatting over the hierarchy GeoNames and the Property schema already model, not AI decisions. "Via" route suggestions for routes with no prior history follow the existing DRAFT/unverified pattern, capped at one or two via-stops rather than an unbounded list. *Why:* explicit ask for a shared, reusable search component that treats AI as a fallback, not a default — protecting rate limits and keeping the common case (a place already known to the app) instant and free.
- **2026-09-16 — Rewrote §18 to actually teach, not just assert.** The previous pass named Spring AI, LangChain4j, Vercel AI SDK, and MCP without explaining what any of them *are* — a fair complaint that it read as jargon dropped on someone explicitly learning this. Added §18.0, a from-zero primer defining every term used in the section in plain language (LLM, model vs. provider, local vs. cloud, what Spring AI/LangChain4j/Vercel AI SDK/MCP each actually do, circuit breaker, rate limiter, ensembling) before any of the technical decisions that use them. Added a screen-by-screen table mapping AI to the actual pages already built (`RouteComparison`, `ItinerarySection`, the locality picker, `FactsBoard`, the Dashboard) instead of leaving it abstract, and rebuilt the Mumbai→Kerala scenario as a full 9-step labeled diagram with a plain-English explanation under every single step. *Why:* explaining architecture to someone building AI literacy through this project only works if every term is defined before it's used — that's the same "no prior background required" rule [§1](#1-what-this-project-is) already set for the rest of this document, which this section hadn't actually been following.
- **2026-09-16 — Designed the multi-provider AI architecture (fallback chain, rate limiting, health-status widget) and corrected the "more AI = more accuracy" assumption with real research.** Full diagram: `AiProviderRouter` tries Ollama → Groq → Mistral in priority order (a documented Spring AI + Spring Retry cascading-fallback pattern, not bespoke), each wrapped in the exact same Resilience4j circuit-breaker/rate-limiter/retry pattern already established for master-data refreshes in §17.3 — reused, not reinvented. A small Actuator-style `HealthIndicator` per provider feeds a `/ai/providers/status` endpoint, which a deliberately compact Dashboard widget (name + status dot + Refresh button) polls — kept light per the Dashboard's own one-viewport rule. Separately, researched whether configuring multiple AI providers actually improves accuracy: the honest answer is **resilience, not accuracy, by default** — multiple providers keep the app working when one is down or rate-limited, but genuine accuracy gains require deliberately calling more than one provider for the *same* request and comparing/flagging disagreement (real, researched LLM-ensembling technique), which costs real multiples of rate-limit budget and is only recommended here for the highest-stakes draft (structured trip extraction), not every AI call. *Why:* the user asked to be corrected if the accuracy assumption was wrong, and it was — partially; the honest answer distinguishes what's automatic from what needs to be deliberately designed.
- **2026-09-16 — Researched the actual AI framework layer for a Spring Boot/React stack (§18) — not just which AI provider, but which library talks to it.** Confirmed **Spring AI 2.0 went GA in mid-2026** and requires Spring Boot 4.0/4.1 + Java 21+, which this project's stack (Spring Boot 4.1.1, Java 25) already exactly satisfies — the version blocker that justified deferring it in [ADR-004](docs/decisions/ADR-004-ai-provider-abstraction.md) no longer exists, only "no call site yet" still does. Its structured-output feature (typed Java objects back from a model, not raw text) maps directly onto this project's own `AiSuggestion`/`AiRecommendation` DRAFT shape from §16. Compared it against LangChain4j (the framework-agnostic alternative, wider provider coverage, faster/breakier releases) and judged Spring AI the better fit specifically *because* this project is already all-in on Spring Boot everywhere else. Also researched MCP (Model Context Protocol, now Spring AI's native tool-calling mechanism) and the Vercel AI SDK for React — and explicitly recommended **against** adopting the latter, since it assumes AI calls happen in a Node/Next.js server, which conflicts with this project's stricter rule that AI only ever runs behind the Spring Boot backend's port. *Why:* "what are enterprise Java/React teams using" deserved a real, sourced answer mapped onto this project's actual constraints, not a generic list — and part of a good answer here was naming the one popular tool that doesn't fit, not just the ones that do.
- **2026-09-16 — Entry-gate flashes now fade out gradually (not cut off), fire more often, and skew bigger; city page shows its areas immediately with cost-of-living/nearby removed.** The ambient flash keyframe held at peak then dropped to zero over a very short window — that sharp drop is what read as "it just goes." Rewritten so the decline is the long part of the cycle (mirroring the hero flash's own soft tail), cycle duration shortened (9s → 5.5s base) and the flash set grew from 7 to 9 elements with more of them at "softbox" scale (300–420px), for a noticeably busier, more continuous feel. Separately, restructured `CityDetailPage`: areas now render directly on the page (no "Explore areas" click-through — `AreaListPage` removed as now-redundant), and the cost-of-living/nearby-places sections are gone entirely per explicit direction, replaced with a city-specific `FactsBoard` (`cityAreaFacts.ts`) — highest/lowest-rent area, PMC/PCMC split, area+locality counts, all computed from the app's own data, plus one general, stable, non-fabricated note about the city (e.g., known IT-hub areas) explicitly not framed as live news. *Why:* the fade-out and frequency issues were about the shape of the animation curve, not just needing more elements; the Property page change removes a click that was in the way, and a display the user explicitly didn't want, rather than layering another fix on top of it.
- **2026-09-16 — Varied the ambient flashes (softbox-big vs. small-bright, irregular timing); matched the nav wordmark's font to the entry gate.** Grew the ambient side-glow set from 4 uniform elements to 7 with real range: sizes from 100px to 430px (small sparkle vs. photography-softbox-scale), durations from 3.5s to 12s (some cycle noticeably more often than others), and — new — a per-element `--flash-peak` CSS custom property so each glow has its own intensity (0.14–0.3) from one shared keyframe, instead of every flash being identically dim. The header's "The Lifestyle" wordmark was still the default sans font; added `font-display` (Playfair Display) to match the entry gate's heading, which uses the same font. Left the hero flash and the rest of the gate untouched, per explicit "rest is fine, don't disturb." *Why:* "sometimes bigger, sometimes smaller, more or less frequent" needed actual per-element variation, not a uniform loop; a brand wordmark should use the same typeface everywhere it appears unless told otherwise.
- **2026-09-16 — Removed the flash's shape entirely — a blurred diamond was still a diamond.** Blur on a `clip-path` shape only softens the outermost edge pixels; the underlying silhouette stays fully intact and reads as "a diamond flashed," which is exactly what kept showing up despite the blur. Dropped `clip-path` completely — the flash is now a pure `radial-gradient` circle with no hard boundary at all, which is what actually produces "spread, soft, no edges," since a gradient has no shape to begin with. Also slowed and dimmed it substantially: the hero glow went from a punchy 0.5s/near-full-white flash to a gentle 1.6s fade peaking at 50% opacity, and the ambient side glows dropped to a near-invisible 14% peak on a slower 9s cycle — the goal per direct instruction was "the eye shouldn't register that something happened," not a bigger or better-shaped flash. *Why:* the fix in the previous entry treated this as an edge-quality problem (needs more blur); it was actually a category error (any clipped shape, however blurred, is still a shape) — worth being explicit that this was a wrong diagnosis, not just an insufficient one.
- **2026-09-16 — Softened the flash edges with real bloom; centered the trip/city cover title; built the Pune area → locality rent hierarchy.** Flash edges were a crisp geometric cut (`clip-path` alone); added a heavy `blur()` filter on top of the diamond shape so it reads as a soft bloom/glow, matching how camera-flash VFX actually looks (researched — glow/bloom via blur, never a hard-edged shape). Sped up the hero flash (0.7s → 0.5s) and moved the content reveal to start while the flash is still fading (`0.45s` delay → `0.2s`) instead of after, so the handoff reads as one moment. `Cover`'s title is now dead-center (both axes) instead of pinned to an edge — a deliberate design choice this time, not the earlier double-padding bug. Built a real three-level Property hierarchy for Pune (the same "one worked example first" pattern as Travel's Dubai/Gwalior trips): city → **areas** (tagged PMC/PCMC, sortable by average rent) → **localities** within an area (with pin codes) → a locality detail page where picking a BHK size (1RK–Villa) and toggling add-on services (tiffin, gym) computes a running estimated monthly cost. New generic `RentSortList` component reused for both the area list and the locality list. Bengaluru has no area data yet — `CityDetailPage` only shows the "Explore areas" entry point for cities that have it. *Why:* named precisely — a shape needs an actual blur to look soft, not just to be called soft; the Property IA request from earlier in the session had stayed a stub until this pass gave it its first real depth.
- **2026-09-16 — One big diamond hero-flash first, content reveals as it fades, ambient flashes turned minimal.** Rebuilt the flash shape from round radial circles to true diamonds (`clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%)`), and restructured the sequence: one large, bright diamond flash fires once on mount (`hero-flash`, 0.7s), and the logo/heading/subtext/button's entrance is now timed (`animation-delay: .45s`) to begin as that flash is fading — the flash reveals the content, rather than both starting together as before. The smaller side flashes are now genuinely minimal (opacity capped around 0.35, smaller, less frequent) since they're ambient texture after the real moment, not competing with it. *Why:* "flash reveals the logo" is a sequencing relationship, not a simultaneous one — needed an actual ordered animation, not just a color/shape tweak on the previous approach.
- **2026-09-16 — Actually eliminated the white-flash gap (structural fix, not another color guess); logo back to square; flashes sharpened.** The white flash survived the earlier `body` background fix because the entry gate had since gone near-black — a cream fallback now flashed *brighter* than both endpoints, worse than before. Root cause was structural, not color: `EntryGate` unmounted itself and mounted the app in sequence, leaving a real gap where neither had painted. Restructured so `children` (the whole app) is **always** rendered, with the gate as a `fixed` overlay on top that fades and removes itself — the Dashboard is already fully painted underneath before the gate ever disappears, so there is no gap left to flash regardless of color. Logo reverted from the circular "medallion" back to the same rounded-square mark used in the header (`rounded-2xl`, solid fill, matching `Header.tsx`), just larger and on a dark ring for contrast. Camera flashes rebuilt from blurred flat circles to a sharp radial-gradient core plus a soft outer bloom (bigger, less blurred, reads as a real flash instead of a rounded blob), and their initial delays tightened to under 1.6s so real flashes are the first thing visible, not a slow trickle. *Why:* a persistent bug across two "fixes" needed a structural fix, not a third color guess; a UI element should generally match its counterpart elsewhere unless a change was explicitly asked for, and the logo shape change wasn't.
- **2026-09-16 — Red-carpet-premiere entry gate: near-black background, ambient camera flashes, one unified reveal.** A deliberate pivot away from the light/minimal direction two entries above — explicit new brief: Bollywood-red-carpet vibe, paparazzi camera flashes from both sides, the logo/heading "coming out of" that atmosphere like a film premiere, no boxed card, no green. Background is near-black (`#0b0b0d`), six blurred white circles flash on independently randomized durations/delays (a shared keyframe, varied per element so they don't sync — genuine paparazzi read instead of one uniform pulse). Logo/heading/subtext/button are wrapped in **one** container with a single `entry-reveal` animation (fade + slight scale, 0.8s) — explicitly not staggered per-element, per direct instruction that a one-by-one sequence "will take a lot of time." The "L." mark has a subtle `animate-pulse` glow ring as its one micro-interaction. This is a deliberate, explicit one-off exception to the "reuse the existing brand palette" rule for this one screen only — confirmed with the user this was wanted, not a drift. Confetti colors shifted to white/champagne/gold to match. *Why:* an intro screen earning genuine "wow" was the actual long-standing ask across several rounds; this is the version built specifically for that instead of variations on the calm in-app palette.
- **2026-09-16 — Fixed a real white-flash bug; redesigned the entry gate to a light, minimal background.** `body` had no explicit `background-color` — it defaulted to browser white, which showed for a frame during the entry gate's unmount → Dashboard mount handoff. Set `body { background: #f8f6f1 }` (the app's own cream base), so any such gap now matches instead of flashing white. Separately, replaced the animated linear-gradient background (its `background-position` animation visibly banded/"stretched") with the plain cream base plus two very-low-opacity soft blobs — no dark colors anywhere now, matching the calm, light identity the rest of the app already uses; adjusted the card and text from white-on-dark to ink-on-light to match. *Why:* the flash was a genuine missing CSS declaration, not a timing issue no amount of transition-tuning could have fixed; "minimal" turned out to mean "match the app's own light palette," not a different dark theme.
- **2026-09-16 — Dropped Vanta.js/three.js entirely for a CSS-only gradient background; reverted confetti to a single burst; rebuilt the facts board as a true infinite conveyor.** After two rounds of tuning the WebGL background with no confirmation it was actually visible, stopped guessing at WebGL config and replaced it with a slow-moving CSS gradient plus two soft blurred colour blobs — no library, no canvas, guaranteed to render in any browser. `three`/`vanta`/`@types/three` uninstalled. Confetti reverted from the two-sided "cannon" back to the original single centre burst per explicit preference, and the exit transition shortened back to a quick, minimal fade (300ms) instead of the longer fade+scale. Separately, rebuilt `FactsBoard` from page-based swapping (3 facts replaced by the next 3, all at once) into an actual sliding conveyor: one fact at a time slides out on the left, the others shift up, a new one slides in on the right, looping forever through the whole list (a cloned-tail track with a transition-free snap-back, the standard technique for a seamless infinite carousel) — arrows now step by one, the same motion as auto-advance, instead of jumping between whole blocks. *Why:* a background that might not be rendering isn't worth continuing to tune blind; a page-swap was never going to look like a conveyor no matter how the transition was tuned, because it wasn't one.
- **2026-09-16 — Slowed the facts-board slide from a "blink" to a genuine glide.** 0.7s over 28px was still reading as an instant blink, not a slide — too short a distance moved too fast to actually perceive as motion. Now 1.8s over 70px with a smooth-deceleration easing curve (`cubic-bezier(0.16,1,0.3,1)`, no snap at the end), and the stagger between the three cards widened from 180ms to 350ms so each one's entrance is clearly its own moment rather than a near-simultaneous ripple. Auto-advance interval nudged to 9s to give the now-longer animation room to finish before the next page starts. *Why:* "slow and smooth" has to be measured in real duration/distance, not just a different easing function on the same fast numbers.
- **2026-09-16 — Swapped the entry-gate background from a globe to a brighter network effect; polished the confetti into a two-sided cannon.** The wireframe globe on a near-black background read as "no color" — its line color and background were too close in darkness for real contrast. Switched to Vanta's `NET` effect: glowing mint-teal lines/dots (`#5eead4`) on the actual brand teal (`#143f40`, not a darker near-black variant), same library, same code-split-on-demand approach. Confetti went from one single center burst to a ~700ms two-sided "cannon" (particles fired from both bottom corners each frame, a well-known canvas-confetti pattern for a more polished celebration feel) and the exit transition is now a slower, eased fade+scale (0.7s) instead of an abrupt opacity cut. *Why:* named precisely — "no color" meant low contrast, not literally no color; "smoother/professional" for confetti has a well-established recipe (multi-burst from the sides) rather than a bigger single burst.
- **2026-09-16 — Animated entry gate (Vanta.js/three.js globe + confetti); fixed the facts-board "blink" into a staggered slide-in.** Replaced the flat green entry screen with a live, rotating WebGL globe background (`vanta/dist/vanta.globe.min`, a free MIT wrapper around three.js — no new colors, themed to the existing teal-green palette) and a `canvas-confetti` burst on the "I'm 18+" click before the transition. Both libraries add real weight (~700kB for three.js), so they're loaded via a dynamic `import()` inside `EntryGate`'s effect rather than the main bundle — confirmed via a production build that the app's own chunk stayed ~325kB with three.js split into its own lazy chunk, not bundled into every page. Separately, `FactsBoard`'s page-change "blink" (an instant, whole-block fade) is now a slow (0.7s), staggered right-to-left slide-in per card — each of the three fact cards animates in individually, 180ms apart, instead of all at once. *Why:* both were named precisely — a flat background and an abrupt cross-fade read as unfinished; the fix for "feels cheap" is usually motion with real timing, not more color.
- **2026-09-16 — Found why the entry gate stopped appearing (persisted forever); redesigned the journey map to a clean vintage look; fixed facts-board layout jump and emoji placement; added auto-advance.** `EntryGate` wrote a `localStorage` flag on the first "Enter" click and never showed again after that — this was a real design mistake, not a rendering bug: an entry screen that wraps the router (so in-app navigation never re-triggers it) doesn't need to persist across page loads at all. Removed the persistence entirely; a fresh load always shows it now. Redesigned `JourneyMap` from scratch: dropped the circular badge around the flag, dropped the four scattered vehicle-emoji "stickers" (they read as cartoonish, not vintage), replaced with a single faint watermark ring and an inset vignette on aged-cream paper — restrained rather than busy. `FactsBoard`: gave each fact card a fixed minimum height so the board's height no longer jumps between pages, added a fade transition on page change, embedded emoji inline inside each fact's sentence instead of as a separate leading icon column (dropped the `Fact.emoji` field entirely — a fact is just a sentence now), and added auto-advance every 7s that resets on manual navigation. *Why:* several of these were plain misses (the gate, the layout jump) worth naming precisely rather than re-polishing around them; "vintage" reads as restraint, not more decoration.
- **2026-09-16 — Eliminated Dashboard scrolling entirely; fixed cover-title alignment; generic `FactsBoard` now on both Travel and Property; journey map restored to red flags plus old-era sticker decoration.** `AppShell` is now route-aware: the Dashboard (`/`) is pinned to exactly one viewport height with `overflow-hidden` and no footer, everything else scrolls normally. `Cover`'s title was indented by 7% twice — once from its own padding, once from the `SectionPanel` it sits inside — so it never lined up with the headings below it; fixed by removing the compounding padding. The Travel-only facts pod became a generic `components/FactsBoard.tsx` (shared, per the generic-first rule), now used by Property too via a new `cityFactGenerators.ts` (rent-share and cheapest-city-to-live-in facts, computed from the existing cost-of-living data); replaced its shuffle button with left/right pagination arrows and fixed emoji/text baseline alignment. `JourneyMap`'s waypoint icon is back to the red flag emoji (🚩) instead of the Heroicons pin — a deliberate exception to the "one icon library" rule for this specifically decorative, old-map-themed component — plus four faint background "stickers" (🐎🐂🚂⛵) for period texture. *Why:* each of these was a specific, named miss — worth fixing precisely rather than broadly re-polishing.
- **2026-09-16 — Corrected the Groq key-expiry claim after seeing the real console; flagged a live key pasted into chat as compromised.** §22.2 had said Groq keys have "no expiry" — the actual console shows a real expiration date per key (~90 days, a rotation practice), which is different from the account/tier itself expiring; corrected the wording to say both things precisely instead of conflating them. Separately, a real secret key was pasted into the conversation while working through this checklist — not written to any file here, but flagged immediately as needing rotation, since a secret that's touched an insecure channel should be treated as compromised regardless of whether it was stored anywhere. *Why:* a claim should only stand once it's been checked against the real thing, not just documentation; a pasted secret is a real security event, not a formatting issue, and deserved being named as one plainly rather than quietly worked around.
- **2026-09-16 — Added an account-setup checklist (§22), filtered against "no trials, ever"; split the ER diagrams into smaller/bigger pieces.** Verified signup steps and card requirements for GeoNames, data.gov.in, Groq, and Mistral individually — all four are genuinely free with no expiring trial, though GeoNames needs an easy-to-miss second step (explicitly enabling web services after signup, not just creating an account). Google Gemini's free tier is real today but was **deliberately left out** of the recommended list: its March 2026 policy shift requires new accounts to set up prepaid billing even for free-tier use, which is close enough to the "eventually needs a card" pattern that it wasn't included without that being a conscious choice. OpenRouter and Cloudflare Workers AI were named but explicitly flagged as not independently re-verified this pass, rather than presented with the same confidence as the four that were checked. Separately, the two ER diagrams from the previous entry were still hard to read — split each into two smaller diagrams (masters vs. a plan's owned data) with attributes moved out into the existing tables and font size roughly tripled (14px → 22px), rather than trying to fit both a full attribute list and bigger text into one diagram at once. *Why:* "no trials" needed to be checked provider-by-provider, not assumed from a category; a diagram trying to show both maximum detail and maximum legibility at the same time can't do either well.
- **2026-09-16 — Researched and grounded §16–§21 with real sources; fixed the ER diagrams; added the price-history and failure-handling design explicitly.** The two ER diagrams were plain relationship graphs with no dark-mode theming and no visible columns — added `primaryColor`/`primaryTextColor`/`lineColor` theme variables matching the rest of this document's diagrams, and gave every entity a short attribute block (PK/FK + a few defining columns), which is what a real ER diagram is expected to show. Verified rather than assumed: [Resilience4j](https://www.baeldung.com/spring-boot-resilience4j) as the current (Hystrix is in maintenance mode) standard for retry/circuit-breaker/rate-limiter/bulkhead around external API calls; GeoNames' actual free-tier limits (~20,000–30,000 credits/day); and a sourced 2026 comparison of standing-free AI APIs (Gemini, Groq, OpenRouter, Mistral, Cloudflare Workers AI) alongside Ollama, which stays the default. Made explicit, as its own named rule (§17.1), what was previously only implied: a refresh upserts masters per-record and appends snapshots, it never deletes or batch-replaces, and a partial failure leaves existing data intact rather than corrupting it. Added §17.2 turning the append-only pattern into the actual "was ₹X, now ₹Y, up N%" UI mechanism that was asked for, computed from two still-existing snapshot rows. *Why:* diagrams that aren't legible aren't diagrams; "AI era, so we'll use AI APIs" deserved real, current numbers instead of a generic gesture at "AI APIs exist"; and a refresh mechanism that's safe by design needs the failure case specified, not just the happy path.
- **2026-09-16 — Planned the backend data model, master-data lifecycle, AI's concrete role, and an honest validation strategy (§16–§21).** Full master/transaction/audit table design for both Travel and Property (ER diagrams for each), a two-trigger (on-demand + scheduled) master-refresh mechanism naming real free sources per master (GeoNames for cities, data.gov.in for Indian pincodes, Frankfurter for exchange rates), a direct answer to "how does AI help with a route I already researched myself" (structured extraction into a draft, never auto-saved), a plain statement of what free data genuinely can't provide (live transport schedules/fares, real rental inventory — both start manual, on purpose), a small Settings page (currency + refresh masters, no account fields), and a phased sequence tying all of it together. None of this is built — it's design, explicitly asked for as planning, not implementation. *Why:* the frontend's static data was always meant to be swapped for a real source later; this is what makes that swap a data-source change instead of a redesign, and it was worth naming the free-data gaps honestly rather than implying a free source exists for everything.
- **2026-09-16 — Synced the structural sections, not just the decision log.** The decision log had been kept current through every round of feedback this session, but §3 (information architecture), §9 (technology stack) and §15 (functionality chapters) had drifted — §3's sitemap still showed the old proposed/dashed folder concept and a since-removed "AI Planner" nav item; §9 was missing Heroicons and canvas-confetti; §15 had only the two original backend chapters despite a full Travel and Property frontend having been built since. Rewrote §3's sitemap/navigation/content-hierarchy to match the real routes and pages, added the missing frontend dependencies to §9, reframed §12 to say plainly that the frontend is now well ahead of the backend rather than letting "Phase 1" imply "not much built," and added functionality chapters 15.3–15.7 covering the entry gate, Dashboard, Travel's route comparison + journey map, Property's area/locality drill-down, and the shared `FactsBoard`. *Why:* a document that's accurate about decisions but stale about current structure and capabilities isn't actually a reliable onboarding entry point — this was a live gap, not a hypothetical one.
- **2026-09-16 — Fixed the missing Bangalore image (a real CSS bug); added a "Did you know?" facts board.** Root cause of the missing image: `background-image: url(${image})` was unquoted, and the Bangalore file's name contains literal parentheses — CSS's `url()` parser treats an unescaped `)` as closing the function early, silently truncating that one URL. Fixed everywhere this pattern appears (`FolderCard`, `Cover`) by quoting the URL; also swapped to a parentheses-free, higher-resolution Bangalore file as a second layer of safety. Added `TravelFactsBoard` below the trip list — a shuffleable "Did you know?" pod computing genuine facts *from the app's own trip data* (e.g. "via Mumbai beats via Delhi by ₹X", cheapest vs. priciest trip). **Deliberately did not** build the weather-alert-style facts ("Mumbai on red alert") as literally described, since that would mean presenting fabricated real-world claims as if true — a live weather feature is possible for real via Open-Meteo (already the project's documented free weather source, no API key, keyless CORS-friendly client fetch) but wasn't built without confirming that's wanted, per the project's own AI/data-honesty rules. *Why:* a genuinely fixed bug is worth explaining, not just patching silently; a "facts" feature has to actually be facts.
- **2026-09-16 — Correction: moved "last checked" to the right spot; adopted Heroicons; made the journey map proportional and more decorative.** "Last checked" had been placed as an overlay inside the hero — moved it to its own section below the hero, in the exact spot the old overview grid occupied, which is where it was actually asked to go. Replaced every mixed emoji icon (nav, folder cards, journey-map pins) with `@heroicons/react` (the Tailwind team's own icon set — minimal, one consistent line style, pairs naturally with the Tailwind adoption). `JourneyMap` segment lines are now proportional to that leg's duration (`flexGrow` set to the duration in hours, so a 15h leg draws visibly longer than a 4h leg) and span the full frame edge-to-edge instead of floating in the middle; added a double-border "map frame" and uppercase serif waypoint labels for more of an old-cartography feel, still using only existing brand tokens. *Why:* the placement and icon inconsistency were real misses worth naming plainly rather than glossing over; the journey map's proportional lines are what actually make it read as a comparison rather than decoration.
- **2026-09-16 — Correction: un-hid the scrollbar; made the Dashboard fit one view by cutting content, not by hiding chrome.** The global scrollbar-hiding CSS from the previous entry was the wrong fix — reverted it, so pages that are genuinely long (trip/city detail) show a normal, visible scrollbar again. For the Dashboard specifically, removed the separate "Your overview" folder-grid section entirely (Travel/Property are already one click away via the top nav, which now has icons) and replaced it with a compact "Last checked" recap tucked into the hero's corner — the Dashboard is now just the hero section, short enough to fit a normal viewport without scrolling. *Why:* hiding scroll chrome everywhere was solving the wrong problem — the actual ask was "make short pages short," not "hide scrolling on long pages too."
- **2026-09-16 — Added an old-map-style journey trail below the route comparison.** `JourneyMap` — deliberately **not** a real geographic map (no map-tile library, no coordinates): rounded flag-pin waypoints (Pune → Mumbai → Gwalior) joined by a dashed trail, each leg captioned with its currently selected mode, fare and duration, on a parchment-toned panel. Pure CSS/emoji, no new dependency, reuses the same `selection` state as the leg picker so it always reflects what's actually chosen. *Why:* explicit ask for an illustrative, old-map feel rather than a real map — and a real map would need a mapping library, tile provider and licensing consideration this project has deliberately deferred (see [docs/free-data-sources.md](docs/free-data-sources.md)).
- **2026-09-16 — Fixed the folder-card image hairline seam; hid scrollbar chrome app-wide.** The image-backed `FolderCard` had `border-2 border-transparent` combined with `rounded-2xl` and a `background-image` — a known browser rendering quirk where a transparent border plus a curved corner plus a background-image can show a faint seam at the edge, from anti-aliasing mismatch between the border's and the background's rounded-corner clipping. Image cards now have **no border at all** (only the plain icon cards keep one) plus `overflow-hidden` and `bg-clip-padding` as a second line of defense; applied the same hardening to `Cover`. Also hid the browser's scrollbar chrome globally (`scrollbar-width:none` / `::-webkit-scrollbar{display:none}` on `html`) per explicit feedback — content is still reachable by wheel/keyboard, only the visible draggable bar is gone, everywhere in the app, not just the Dashboard. *Why:* borders don't belong on a photo card in the first place — the image itself provides the edge definition; a visible scrollbar track wasn't wanted anywhere.
- **2026-09-16 — Route comparison rework: side-by-side routes, auto-best-on-sort, dates, and a number-legibility fix.** Fare/duration totals were rendering in Playfair Display (the display serif) — its stylized digits were the "numbers aren't recognizable" complaint; switched all fare/duration figures to DM Sans with `tabular-nums`. Added `RouteCompareStrip` (via-Mumbai vs. via-Delhi shown as cards side by side, cheapest/fastest badge, informed by how Google Flights surfaces a "best" option) so both routes are compared at a glance instead of hidden behind tabs. Sorting by fare/duration now auto-selects the cheapest/fastest mode on every leg (not just reorders the option buttons) — exactly what was asked for. Added `TripDates` (one-way/round-trip toggle, depart/return date pickers defaulting to today/tomorrow, going/returning/combined fare breakdown) — the return leg mirrors the outbound route's fares rather than being independently priced, called out in the UI. Increased Wikimedia image resolution and added `bg-no-repeat` to the cover banner and folder-card thumbnails as a robustness fix for a reported image-fit issue that couldn't be reproduced from this sandbox (no outbound network access here to inspect the rendered image). *Why:* the previous tab-based comparison hid the exact side-by-side view asked for, and per-leg sorting without changing the total defeated the point of sorting at all.
- **2026-09-16 — Adopted Tailwind CSS and reorganized `lifestyle-web` into small, single-purpose components.** Replaced the hand-written `styles.css` (previously ~650 lines of manual rules) with Tailwind v4 (`@tailwindcss/vite`) — `styles.css` is now just the font import plus a `@theme` block defining the brand's colors/fonts as design tokens; every component styles itself with Tailwind utility classes, custom CSS reserved for the brand tokens only. Split what were a few large files (`App.tsx`, `TripDetail.tsx`, `CityDetail.tsx`) into small, single-purpose ones under `layout/` (Header/Nav/Footer/AppShell), `pages/dashboard/` (Hero/Overview/Page), and per-feature `features/travel|property/components/` (e.g. `LegPicker`, `FareSummary`, `RouteComparison`, `CostOfLivingSection`), plus shared generic components (`FolderCard`, `Cover`, `DataRow`, `Eyebrow`, `SectionPanel`) reused across Travel and Property per the generic-first rule in [§10](#10-non-negotiable-engineering-rules). Verified with lint/test/build and the Vite dev server compiling every file, including confirming Tailwind's utility CSS actually generates (`brand-950` etc. present in the served stylesheet). *Why:* explicit ask — enterprise-style organization a larger team could actually divide work across, and a CSS approach that doesn't keep growing one manually-maintained file.
- **2026-09-16 — Expanded the Phase 1 UI concept: fare/route comparison, Property first draft, dashboard rework.** Gate copy made casual/Gen-Z. Trip folders and cover banners now use real Wikimedia Commons photos (hotlinked via `Special:FilePath`, not scraped or hosted — CC-BY/CC-BY-SA licensed, attribution still owed if this ever ships publicly). Route comparison redesigned: each leg now has multiple mode options (flight/train/road) with illustrative fare + duration, sortable, with a running total-fare/total-duration summary panel — all client-side, deterministic arithmetic, no live pricing. Itinerary is now optional (hidden by default, toggled on). Removed the "AI Planner" top-level nav item — per direction, AI isn't a separate section, it integrates into Travel/Property later. Removed the Dashboard's live service-status panel, replaced with a generic "last opened" indicator per section (`lib/lastAccessed.ts`, localStorage-backed). Added a first-draft Property IA: city folders → cost-of-living breakdown + nearby places, same `FolderCard`/pattern as Travel, explicitly marked as a draft since Property's shape was asked for as an open design exercise, not specified. Verified with lint/test/build and by fetching every new module from the running Vite dev server. *Why:* this is a genuinely different, richer product surface than the first cut — logged as one entry since it's one connected direction, not several unrelated changes.
- **2026-09-16 — Built the entry gate and folder navigation for real.** Implemented in `lifestyle-web`: `EntryGate` (age-gate wrapping the app), a generic `FolderCard` (double-click to open, reused for Dashboard's Travel/Property and for trip folders — per the generic-first rule in [§10](#10-non-negotiable-engineering-rules)), and `TravelFolder`/`TripDetail` with the Pune→Gwalior (via Mumbai/via Delhi) and Pune→Dubai worked examples as static placeholder data. Resolved the open "literal vs. stylized desktop metaphor" question pragmatically: stylized CSS/React, not simulated OS windows. Verified with `npm run lint`, `npm test`, `npm run build`, and the Vite dev server compiling every new module without error — **not** verified with an actual screenshot, since no browser-automation tool is available in this environment. *Why:* the concept had been documented twice but never actually built, which is what the user was (rightly) pushing back on.
- **2026-09-16 — Added a diagram style guide, applied per diagram type.** Full-project/microservice/data diagrams now use orthogonal connectors (`curve: 'stepAfter'`); the AI-strategy feature-flow diagram is top-to-bottom with curved connectors; the primary user-flow diagram uses rounded "card" (stadium) nodes. Documented as a table right after the intro, and saved as a standing memory rule for future diagrams. *Why:* different diagram purposes read better with different conventions (a user journey isn't drawn like a microservice's internals) — this makes that explicit instead of ad hoc per diagram.
- **2026-09-16 — Added §3, information architecture.** New sitemap/page-hierarchy diagram (solid = built, dashed = proposed), a navigation-model table (primary/secondary/contextual), a content taxonomy grounded in the existing domain model, and content hierarchy for the Dashboard (real) and a Trip folder (proposed). *Why:* IA should be explicit and diagrammed, not implied by the code — and keeping "real" vs. "proposed" visually distinct stops the document from overstating what's actually built.
- **2026-09-16 — Fixed the broken site-map diagram and cleaned up crossing lines/wasted space.** The mind map used Mermaid's native `mindmap` type with per-node `:::class` styling, which failed to render for some viewers — rebuilt it as a flowchart "sticky-note board": each branch is one outlined single-border group (Travel/Property/AI), with small yellow sticky-note tiles inside instead of separate boxes wired by crossing lines. Also regrouped the system-overview diagram (DB + optional AI now share one row instead of AI's dashed lines crossing over the database box) and the two hexagonal/AI-provider diagrams (paired core nodes now sit inside one thin outline border instead of floating separately), and switched a couple of diagrams to left-to-right to use the page's horizontal space instead of stacking narrowly. *Why:* a mind map that doesn't render is a bug, not a style choice; grouping related nodes inside one light border reads cleaner than either loose floating boxes or one heavy solid-filled box, and fixes most of the line-crossing.
- **2026-09-16 — Diagram polish: brighter colors, varied shapes, visible connectors, emoji back on the mind map.** Swapped near-black fills for brighter, saturated ones; added stadium/hexagon shapes alongside rectangles instead of uniform boxes; set an explicit bright line color so arrows are visible on dark backgrounds; restored emoji specifically on the site-map mind map and primary-flow diagram (kept off the four core architecture diagrams for a cleaner look). *Why:* dark near-black fills and default line color were hard to read; varied shapes and a bit of icon color make the UX-facing diagrams easier to scan without cluttering the architecture ones.
- **2026-09-16 — Simplified this document.** Cut emoji from every diagram and heading, unified all diagrams to one consistent theme/palette, shortened every section significantly. *Why:* an earlier pass got cluttered and inconsistent (mixed themes, heavy emoji, bloated prose) — this is a plain readability fix, not a content change.
- **2026-09-16 — Added §2 (audience/UX) and reframed diagrams as C4 Context/Component levels.** *Why:* orienting readers with "who is this for" before the technical detail, and naming the diagram convention, both make the document easier to navigate.
- **2026-09-16 — Added Swagger/OpenAPI, statelessness and generic-first as explicit rules.** `springdoc-openapi` 3.1.1 added to both services; §9 now states statelessness and generic-first reuse as rules, not assumptions; added [§15, functionality chapters](#15-functionality-chapters). *Why:* enterprise-standard API docs with zero manual upkeep, and principles that were implicit deserve to be written down. *Unverified:* backend compile — this machine has Java 8, the project targets Java 25.
- **2026-09-16 — Correction: reuse the existing green brand, don't replace it.** [docs/product-concept-ui.md](docs/product-concept-ui.md) wrongly proposed a maroon/red scheme; `lifestyle-web/src/styles.css` already has a real teal-green identity, now reused instead. *Why:* an existing, liked brand should never get silently replaced.
- **2026-09-16 — Proposed entry gate + folder-style navigation.** Captured, not decided, in [docs/product-concept-ui.md](docs/product-concept-ui.md) — an 18+ tone-setting gate, then desktop/folder navigation, with Dubai and Pune→Gwalior worked examples. *Why:* worth preserving, but it's a real IA change that needs a decision before Phase 2 UI work follows it.
- **2026-09-16 — Architecture, tech stack, and this document.** Hexagonal architecture per service, two independent services, schema-isolated PostgreSQL, optional/pluggable AI. *Why:* practicing real team patterns at small scale is the project's learning goal.
- **2026-09-16 — Multi-tool agent context.** `AGENTS.md` is the single canonical instruction source; `CLAUDE.md`, `.github/copilot-instructions.md`, `.cursor/rules/project.mdc`, `.clinerules` are thin pointers to it, never duplicates. *Why:* switching tools shouldn't mean re-explaining the project or risking contradictory copies.

---

## 14. Looking ahead

Not commitments — the honest answer to "how would this grow." Nothing here is scheduled; see [docs/roadmap.md](docs/roadmap.md) for what's actually planned.

- **Multiple users:** add an owner/tenant id to `TripPlan`/`PropertyPlan` and an auth adapter behind a port — domain logic untouched.
- **More bounded contexts:** a third *business* domain follows the same recipe — its own service, schema, ADR, never a shared table. (`integration-service`, added 2026-09-16, isn't an instance of this — it's an integration layer with no business domain of its own, not a fourth bounded context; see [§18](#18-ais-role-in-this-application).)
- **Real deployment:** swap the dev proxy for a reverse proxy, point at managed PostgreSQL — a config/adapter change, not a redesign.
- **Heavier AI:** ~~the `AiProvider` port has room for a second, paid adapter, opt-in, same draft-and-approve rules~~ — **built**, 2026-09-16: `integration-service` already has three (Ollama, Groq, Mistral) behind exactly this port, with the draft-and-approve `ai_suggestion` table live. What's still ahead: LLM ensembling for higher-stakes drafts (§18's "does more AI mean more accuracy" section) and wiring the fallback into `travel-service`/`property-service`'s own search.
- **Search:** `pgvector` is the documented next step if PostgreSQL full-text search isn't enough — not installed until measured need exists.
- **Data volume:** append-only snapshots grow over time; retention policy is a known future task, not an emergency.

Every item above already has a seam to grow into, because the boundaries were chosen up front — that's the payoff of these patterns: cheap change later, not looking impressive now.

---

## 15. Functionality chapters

What's actually been built, one chapter per capability, in build order. Unlike the decision log (records *choices*), a chapter records a *capability that exists now*.

**Template:** `<name>` — **What (functional):** what a person can now do. **Why:** the problem it solves. **How (technical):** layers touched, pattern reused. **Reusable as a template for:** what future work can copy by only changing data.

**15.1 — Service info endpoint.** *What:* `GET /api/travel/v1/info` (and property's equivalent) returns name, version, status — proof of life for a health check or the dashboard. *How:* `domain/ServiceStatus` (plain record) → `application/ServiceInfoUseCase` (`@Service`) → `adapter/in/web/ServiceInfoController` + DTO (`@RestController`, never returns the domain object directly). *Template for:* every future read endpoint follows this same four-file chain.

**15.2 — API documentation (Swagger UI).** *What:* both services serve a live API reference at `/swagger-ui.html` and a machine-readable contract at `/v3/api-docs`, generated from the controllers. *How:* `springdoc-openapi-starter-webmvc-ui` ([§9](#9-technology-stack)) inspects registered controllers automatically; a `config/OpenApiConfig` supplies only title/description/version. Lives in `config`, not `domain`/`application` — pure framework wiring. *Template for:* nothing to maintain as endpoints are added — that's the point.

**15.3 — Entry gate.** *What:* a full-screen "enter the app" moment on every fresh load (age-gate framing, not real verification) — a soft ambient light effect, a confetti burst on "Enter," then the Dashboard. *How:* `components/EntryGate.tsx` always renders `children` underneath and overlays itself with `fixed inset-0 z-50`, fading out rather than unmount/remount-swapping (that sequencing was the fix for an earlier white-flash bug). No `localStorage` — a fresh page load always shows it, since it wraps the router and in-app navigation never remounts it. *Template for:* any future full-screen overlay that needs to sit above the whole app without disturbing what's underneath.

**15.4 — Dashboard.** *What:* the hero statement, a "Last checked" recap linking back to the last-opened trip/city, and (since 2026-09-16) a compact **AI-model status row** below both — one dot per provider (Ollama/Groq/Mistral), green/red/orange. *How:* `pages/dashboard/DashboardHero.tsx` + `RecentlyChecked.tsx`, reading from `lib/lastAccessed.ts` (a small generic localStorage helper, `section: 'travel' | 'property'`); `features/dashboard/AiStatusWidget.tsx` calls `integration-service`'s `GET /api/integration/v1/ai/providers/status` directly (the first place `lifestyle-web` calls `integration-service`, not just `travel`/`property`) and renders nothing at all — not an error, just nothing — if that service isn't reachable, so a missing backend never breaks the Dashboard. `layout/AppShell.tsx` is route-aware — the Dashboard (`/`) alone is pinned to one viewport height with no scroll, everything else scrolls normally; the status row is deliberately one compact line to respect that. *Template for:* `lastAccessed` is reused as-is by Property; any future "recently viewed X" feature follows the same read/record pair. Verified: `npm run lint`/`test`/`build` all pass; not verified against a running `integration-service` (§12).

**15.5 — Travel: trip folders, route comparison, journey map.** *What:* `/travel` lists trip folders (double-click to open) plus a "Did you know" facts pod; a trip's detail page shows route alternatives (e.g. via Mumbai vs. via Delhi) with a per-leg mode picker (flight/train/road, each with fare + duration), a sort-by-cheapest/fastest control that auto-picks the best option per leg, a running total, an illustrated journey-map trail, and an optional (hidden by default) itinerary. *How:* static data in `features/travel/tripData.ts`; `components/RouteComparison.tsx` + `LegPicker.tsx` + `RouteCompareStrip.tsx` + `JourneyMap.tsx` + `ItinerarySection.tsx`, all under `features/travel/components/`, each single-purpose per [§10](#10-non-negotiable-engineering-rules)'s file-organization ask. *Template for:* §15.6 below reuses this same folder → detail → drill-down shape for Property; a future "AI Planner" surfacing a draft itinerary would plug into the same `ItinerarySection`-style optional-reveal pattern.

**15.6 — Property: city → area → locality drill-down.** *What:* `/property` lists city folders; a city's page shows its areas directly (sortable by rent) plus a city-specific facts pod; an area shows its localities (with pin codes); a locality lets you pick a home size (1RK–Villa) and toggle add-on services (tiffin, gym), computing an estimated monthly total live. *How:* static data in `features/property/cityData.ts` and `areaData.ts`; generic `components/RentSortList.tsx` (shared by the area list and the locality list) + `FolderCard`/`Cover`/`DataRow` reused from Travel. Only Pune has area-level data — `CityDetailPage` checks for it and quietly omits the section for cities that don't (Bengaluru), rather than showing an empty state. *Template for:* adding area data for another city is purely a `areaData.ts` edit — no component changes needed.

**15.7 — "Did you know" facts board.** *What:* a small pod of computed, honest facts (e.g. "via Mumbai beats via Delhi by ₹1,400," "Aundh has the lowest average rent in Pune") that auto-advances and can be paged with arrows, used on both the Travel and Property listing pages and on each city's page. *How:* generic `components/FactsBoard.tsx` (a real sliding conveyor, not a page-swap — see the decision log for why that distinction mattered) takes a plain `Fact[]`; each domain supplies its own generator (`factGenerators.ts`, `cityFactGenerators.ts`, `cityAreaFacts.ts`) that only computes from the app's own static data — nothing is invented, and a real-world claim (e.g. weather alerts) was deliberately left out for exactly that reason. *Template for:* any future "interesting facts about X" surface reuses `FactsBoard` unchanged, supplying only a new generator function.

---

## 16. Planned backend data model — masters, transactions, audit

**Update, 2026-09-16: the schema below is no longer just design — it's real DDL.** Every table described in this section exists in Postgres today, created by Flyway (`V1__phase1_marker.sql` + `V2__domain_tables.sql` in both `travel-service` and `property-service`). What's still true to the word "planned": almost none of it has application code reading or writing it yet. The two exceptions are `city`/`currency` (upserted by `MasterRefreshUseCase`, §20) and, in `integration-service`, an AI-draft-provenance table shaped like `ai_suggestion`/`ai_recommendation` below (§18). Everything else — `trip_plan`, `route_option`, `trip_leg`, `leg_price_quote`, `property_plan`, `rent_snapshot`, `cost_estimate`, and the rest — is real, migrated, and untouched by any use case. The frontend's static `tripData.ts`/`cityData.ts`/`areaData.ts` ([§3.3](#33-content-taxonomy)) already mirror this shape closely, so wiring them to this now-real backend is meant to be a data-source swap, not a redesign. Both services keep their own copy of every table — per [ADR-003](docs/decisions/ADR-003-single-postgres-separate-schemas.md) there is no cross-schema FK, so even a "shared" master like `city` or `currency` is duplicated per schema, each refreshed independently ([§17](#17-master-data-lifecycle)).

**The three kinds of table, defined once, reused for both contexts:**

| Kind | What it holds | Refreshed how | Deleted? |
|---|---|---|---|
| **Master** | Reference/lookup data that's true regardless of any one user's plan — cities, currencies, transport modes, BHK types, pincodes | Pulled/refreshed from an external source or curated list, on a schedule or on demand ([§17](#17-master-data-lifecycle)) | Rarely; superseded, not deleted |
| **Transaction** | A user's own data — a trip plan, a chosen route, a saved cost estimate | Created/edited/deleted by the user (full CRUD) | Yes — a trip or property plan is the user's to delete |
| **Audit** | A record of what changed, when, and by what | Written automatically alongside every transaction-table write | Never — append-only, same principle as [ADR-005](docs/decisions/ADR-005-price-snapshot-strategy.md)'s price snapshots |

One generic `audit_log` table per service (not one audit table per transaction table) — `(entity_type, entity_id, action, changed_at, changed_by, diff_json)` — per the [generic-over-bespoke rule](#10-non-negotiable-engineering-rules): every future transaction table gets audit coverage by writing to the same table, not by adding a new one.

### 16.1 Travel schema

Split into two smaller diagrams on purpose — one dense diagram trying to show every table *and* every column was part of why the last version was hard to read. Columns live in the table underneath instead.

**Master data, referenced by the transaction tables:**

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'22px','primaryColor':'#164e63','primaryTextColor':'#ecfeff','primaryBorderColor':'#67e8f9','lineColor':'#5eead4'}}}%%
erDiagram
    CITY ||--o{ TRIP_LEG : "from / to"
    TRANSPORT_MODE ||--o{ TRIP_LEG : "mode of"
    CURRENCY ||--o{ LEG_PRICE_QUOTE : "priced in"
    VISA_REQUIREMENT ||--o{ DOCUMENT_REQUIREMENT : sources
```

**A trip and everything it owns:**

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'22px','primaryColor':'#164e63','primaryTextColor':'#ecfeff','primaryBorderColor':'#67e8f9','lineColor':'#5eead4'}}}%%
erDiagram
    TRIP_PLAN ||--o{ ROUTE_OPTION : has
    ROUTE_OPTION ||--o{ TRIP_LEG : has
    TRIP_LEG ||--o{ LEG_PRICE_QUOTE : "has snapshots"
    TRIP_PLAN ||--o{ ITINERARY_DAY : has
    TRIP_PLAN ||--o{ DOCUMENT_REQUIREMENT : requires
    TRIP_PLAN ||--o{ AI_SUGGESTION : "drafted for"
```

| Table | Kind | Key columns (beyond id/timestamps) |
|---|---|---|
| `city` | Master | name, country_code, lat/long, timezone — refreshed from GeoNames |
| `currency` | Master | iso_code, symbol, exchange_rate_to_base, rate_captured_at — refreshed from Frankfurter |
| `transport_mode` | Master | code (flight/train/road/waterway), label |
| `visa_requirement` | Master | origin_country, destination_country, requirement_text, source_url, verified_at — **manually curated**, no reliable free live source exists ([§19](#19-data-validation--honest-limitations)) |
| `trip_plan` | Transaction | title, status, created_at |
| `route_option` | Transaction | trip_plan_id, label (e.g. "via Mumbai") |
| `trip_leg` | Transaction | route_option_id, from_city_id, to_city_id, transport_mode_id, order |
| `leg_price_quote` | Transaction (append-only, like a price snapshot) | trip_leg_id, currency_id, amount, duration_hours, source, captured_at, confidence |
| `itinerary_day` | Transaction | trip_plan_id, day_number, title, detail |
| `document_requirement` | Transaction | trip_plan_id, visa_requirement_id, verified (bool) |
| `ai_suggestion` | Transaction | trip_plan_id, model, prompt_version, payload_json, status (`DRAFT`/`ACCEPTED`/`REJECTED`) |
| `audit_log` | Audit | entity_type, entity_id, action, diff_json, changed_at |

### 16.2 Property schema

Same split as Travel — masters/geography in one diagram, a plan and what it owns in the other; full columns are in the table below.

**Geography masters:**

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'22px','primaryColor':'#581c87','primaryTextColor':'#faf5ff','primaryBorderColor':'#e9d5ff','lineColor':'#c084fc'}}}%%
erDiagram
    CITY ||--o{ AREA : contains
    MUNICIPALITY ||--o{ AREA : governs
    AREA ||--o{ LOCALITY : contains
    LOCALITY ||--o{ PINCODE : "mapped to"
```

**A property plan and its cost estimate:**

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'22px','primaryColor':'#581c87','primaryTextColor':'#faf5ff','primaryBorderColor':'#e9d5ff','lineColor':'#c084fc'}}}%%
erDiagram
    LOCALITY ||--o{ RENT_SNAPSHOT : "observed in"
    BHK_TYPE ||--o{ RENT_SNAPSHOT : "priced for"
    PROPERTY_PLAN ||--o{ COST_ESTIMATE : has
    LOCALITY ||--o{ COST_ESTIMATE : "estimated for"
    SERVICE_ADDON ||--o{ COST_ESTIMATE : includes
    PROPERTY_PLAN ||--o{ AI_RECOMMENDATION : "drafted for"
```

| Table | Kind | Key columns |
|---|---|---|
| `city` | Master | same shape as Travel's — its own copy, own refresh |
| `municipality` | Master | name, code (e.g. PMC/PCMC), city_id |
| `area` | Master | name, city_id, municipality_id |
| `locality` | Master | name, area_id |
| `pincode` | Master | code, locality_id — refreshed from a postal/pincode dataset |
| `bhk_type` | Master | code (1RK…Villa), label |
| `service_addon` | Master | code (tiffin/gym/…), label, is_active |
| `currency` | Master | shared shape/pattern with Travel, own copy |
| `property_plan` | Transaction | title, status |
| `rent_snapshot` | Transaction (append-only) | locality_id, bhk_type_id, currency_id, amount, source, captured_at, confidence |
| `cost_estimate` | Transaction | property_plan_id, locality_id, bhk_type_id, selected_addon_ids, total_amount, computed_at |
| `ai_recommendation` | Transaction | property_plan_id, model, prompt_version, payload_json, status |
| `audit_log` | Audit | same generic shape as Travel's — own instance |

A real, granular fact like "does Aundh actually have any 4BHK inventory" belongs in `rent_snapshot` presence/absence — if no snapshot exists for a `(locality, bhk_type)` pair, the honest answer is "unknown," not "no" and not a guessed number. [§19](#19-data-validation--honest-limitations) covers this in more depth.

---

## 17. Master data lifecycle

**The mechanism, two triggers, one shared shape:**

- **On-demand** — a person clicks "Refresh masters" in [Settings](#20-settings-frontend), which calls one endpoint per master type (e.g. `POST /api/travel/v1/masters/currency/refresh`). Useful right before planning something, per the "whenever I start working on something" case.
- **Scheduled** — later, Spring's built-in `@Scheduled` (free, no new infrastructure — no Kafka/cron service needed) runs the same refresh logic periodically. Cadence matches how often the real data actually changes: currencies daily, city/pincode masters monthly-ish, visa requirements on a much longer, manually-reviewed cycle since no reliable free live source exists for them.

Both triggers call the same `MasterRefreshJob` per master type — one small port/adapter per master (`CurrencyRefreshJob`, `CityMasterRefreshJob`, `PincodeMasterRefreshJob`, …), each knowing how to pull its one external source and upsert into its master table. A `master_refresh_log` row is written every run (`master_type, started_at, completed_at, records_upserted, status, error_message`) — this is what the Settings screen shows as "last refreshed."

**Built so far (§20) is a smaller, honest first slice of this design, not the whole thing:** one `MasterRefreshUseCase` per service refreshing `city` + `currency` together behind a single `POST /api/{travel|property}/v1/masters/refresh` (not yet split into one job/endpoint per master type), on-demand only (no `@Scheduled` yet), logging to a real `master_refresh_log` table (column named `master_name`, not `master_type`, but otherwise this exact shape). `pincode`/`transport_mode`/visa data are not refreshed at all yet — see the gap called out below and in §20.

**Free sources for the masters that actually have one:**

| Master | Free source | Notes |
|---|---|---|
| `city` (worldwide) | [GeoNames](https://www.geonames.org/) | Free bulk download + a rate-limited free API; the most complete free worldwide place-name dataset |
| `pincode` (India) | [data.gov.in](https://www.data.gov.in/) open postal datasets | Government open data, free, periodically updated |
| `currency` exchange rates | [Frankfurter](https://frankfurter.dev/) | Already the project's documented choice ([docs/free-data-sources.md](docs/free-data-sources.md)); free, no key |
| `country` | [REST Countries](https://restcountries.com/) | Already documented |
| Map tiles (display only) | [OpenStreetMap](https://www.openstreetmap.org/) + [Leaflet](https://leafletjs.com/) | For showing a map later, not for routing data |
| Geocoding | [Nominatim](https://operations.osmfoundation.org/policies/nominatim/) | Usage-policy limits apply — fine for occasional lookups, not bulk |
| Road routing | [OSRM](https://project-osrm.org/) / [GraphHopper](https://www.graphhopper.com/open-source/) | Road distance/time only |

**Masters with no free live source — the honest gap:** `transport_mode` schedules (does a train actually run Pune→Gwalior on a given date), flight fares, and real rent/listing data for `rent_snapshot` all lack a comprehensive free API. IRCTC and airline fare data are effectively paid/restricted; property-listing sites' terms generally prohibit scraping (a hard rule in this project already — [AGENTS.md](AGENTS.md)). These start as **manual entry**, exactly like the illustrative data already in the frontend, and stay that way until a specific free or user-approved-paid source is identified — see [§19](#19-data-validation--honest-limitations).

**A GeoNames-specific limitation, from its own docs:** postal-code coverage for Canada, Ireland and Malta is only the first few characters of the full code (copyright reasons on their end, not ours), Argentina's postal data predates that country's 1999 system change, and Brazil only has one major code per municipality, not full coverage. None of this affects Pune/India, where this project's actual focus is — but worth knowing before assuming GeoNames' pincode data is equally complete everywhere if this ever expands past India.

### 17.1 Refresh means upsert-and-append, never delete

This is worth stating as an explicit rule, not just an implied one: **a refresh never removes a row.**

- **Master tables are upserted, per record.** Refreshing `city` updates the matching row if GeoNames' data changed, inserts a new row if it's a place not seen before, and **touches nothing else**. If GeoNames drops a city from its dataset, this app doesn't delete it — a plan referencing it would break, which is worse than keeping a slightly stale master row.
- **Transaction/snapshot tables never update in place — they append.** A new `leg_price_quote` or `rent_snapshot` row is *inserted* with a fresh `captured_at`; the previous one stays exactly as it was, forever. This is [ADR-005](docs/decisions/ADR-005-price-snapshot-strategy.md)'s rule, generalized to every observed price in both schemas, not just Travel's.
- **Every write is scoped to one record, not a batch replace.** A `MasterRefreshJob` loops record-by-record (or in small transactional batches) — one bad record from the source fails and gets logged, the other 4,999 still update. There's no "truncate then reload" anywhere in this design, because a truncate-then-reload that fails halfway is exactly the kind of silent data loss this rule exists to prevent.

### 17.2 Price history — what changed, without losing what it was

The append-only rule above is what makes this possible: because `leg_price_quote` and `rent_snapshot` never overwrite, the *previous* value is always still there to compare against.

- A `price_history` read model (a query, not a new stored table) takes the two most recent snapshots for the same `(trip_leg, mode)` or `(locality, bhk_type)` and computes the difference — deterministic Java, the same "money math is never AI" rule as everywhere else in this project.
- The UI surface for this is exactly what you described: *"was ₹4,200 on 12 Sep, now ₹4,650 — up 10.7%."* It reads directly off two rows that both still exist; nothing was deleted to produce that sentence.
- This is also precisely the input to the AI price-change narration use case in [§18](#18-ais-role-in-this-application) below — Java computes the number, AI only turns it into a sentence.

### 17.3 What happens when a refresh call fails

Researched against current (2026) practice for calling external APIs from a Spring Boot service — the standard toolkit is [Resilience4j](https://www.baeldung.com/spring-boot-resilience4j) (the successor to Netflix Hystrix, which is in maintenance mode), not something bespoke:

| Failure pattern | What it does | Applied here to |
|---|---|---|
| **Retry** | Retries a failed call a bounded number of times, only for transient/network errors — never for a 4xx, since retrying an unrecoverable error just wastes time | A `MasterRefreshJob` hitting GeoNames/Frankfurter and getting a timeout |
| **Circuit breaker** | After enough consecutive failures, stops calling the failing source for a cooldown window instead of hammering it, then tries a limited number of test calls before fully reopening | Protects this app (and the free API's rate limit) if GeoNames is down or the free quota is exhausted |
| **Rate limiter** | Caps outgoing calls to stay under a source's published quota | GeoNames specifically — its free tier is capped (documented as roughly 20,000–30,000 credits/day, ~1,000–2,000/hour, varying by source) — worth respecting so the app never gets the key blocked |
| **Bulkhead** | Isolates one slow external dependency so it can't starve resources needed by the rest of the service | Keeps a slow visa-data lookup, for instance, from blocking Travel's other endpoints |

**Concretely, when a refresh fails partway:** the `master_refresh_log` row for that run is marked `FAILED` with the error message, the master table keeps whatever it successfully upserted before the failure (§17.1's per-record rule) plus everything from the last successful run, and the Settings screen shows the failure plainly rather than silently pretending the refresh succeeded. Nothing user-facing breaks — a stale-but-present master is always safer than a missing one.

---

## 18. AI's role in this application

### 18.0 Start here — AI, in plain terms, no background assumed

Everything after this point uses these words as defined here. Read this once; the rest of the section will make a lot more sense.

- **What an LLM actually is.** A "Large Language Model" is a program trained on enormous amounts of text that got very good at one specific skill: given some words, predict what words should come next in a way that sounds right. It is *not* a database — it doesn't "look up" facts, it generates plausible-sounding language. That's exactly why this project has a hard rule that AI never computes money, never asserts a fact as true ([§10](#10-non-negotiable-engineering-rules)) — it's genuinely excellent at *language*, and genuinely bad at being trusted as a source of truth.
- **"Model" vs. "provider."** A **model** is one specific trained "brain" — Llama, Mistral Small, whatever. A **provider** is the company that lets you send text to a model and get a reply — some run their model on *their own* servers (Groq, Mistral, Gemini); one runs it *on your own computer* (Ollama).
- **Local vs. cloud, what that actually means for you.** With **Ollama**, the model runs on your laptop — nothing you type ever leaves your machine, it's free forever, but it's only as fast/capable as your hardware. With **Groq** or **Mistral**, your text travels over the internet to their servers and back — usually faster and more capable, but you're working inside a free daily/per-minute limit before they'd start charging.
- **What "Spring AI" actually is.** Without it, talking to Ollama needs one style of code, talking to Groq needs a *different* style, Mistral a third. Spring AI is a library that gives Java **one consistent way** to say "send this text to a model, give me the answer back" — and it handles the provider-specific details underneath. Think of it as a universal remote control, for AI, written for Java — made by the same team that builds Spring Boot, which already runs this whole backend.
- **What "LangChain4j" is.** The same idea — a universal remote for AI in Java — built by a different, independent community, and it also works with other Java frameworks besides Spring. We're using Spring AI instead because our backend is *already* 100% Spring Boot; Spring AI plugs into the exact patterns we already use everywhere (dependency injection, health checks), so there's less new stuff to learn just to use it. LangChain4j would be the right pick for a project that *wasn't* already all-in on Spring — that's a real, specific reason, not a coin flip.
- **What the "Vercel AI SDK" is.** The equivalent universal remote, but for JavaScript/React, made by Vercel (the company behind Next.js). We're **not** using it, and the reason is concrete, not a preference: it assumes the AI call happens directly inside a Node.js server. In this project, the AI call is only ever allowed to happen inside the Java backend — that's the rule that makes "a human must approve every AI draft" actually enforceable. A tool built for a different shape of backend doesn't fit ours, however good it is in general.
- **What "MCP" (Model Context Protocol) is.** Imagine letting the AI *look something up itself* — say, today's exchange rate — instead of you feeding it every fact. MCP is a standard way to hand a model a fixed, safe list of things it's allowed to check (called "tools"), so it can only ever do exactly those specific, approved lookups — never anything else. Nothing in this app uses this yet; it's the modern standard for *when* that day comes.
- **"Circuit breaker" and "rate limiter," in plain terms.** A circuit breaker is like a fuse box — if calling a provider keeps failing, it "trips" and stops hammering it for a while instead of retrying forever. A rate limiter is a queue manager — it makes sure this app never sends more requests per minute than a free provider allows, so it never gets temporarily blocked for going over the limit.
- **"Ensembling," in plain terms.** Asking the *same* question to two different AI "brains" and comparing their answers — if they agree, more confidence; if they don't, a flag to double-check. Like asking two friends the same question before trusting the answer, at the cost of asking twice.

### Where AI touches the app you've actually already built, screen by screen

Not abstract — mapped onto the real, running pages from [§15](#15-functionality-chapters):

| Screen (already built) | What AI would add there | Status |
|---|---|---|
| Trip detail — `ItinerarySection` | Draft the day-by-day itinerary from a one-line description, instead of you typing each day | Planned, not built |
| Trip detail — `RouteComparison` | Turn "Mumbai to Kochi by flight, then Munnar by road" into the actual leg rows this screen already displays and lets you compare | Planned, not built |
| Locality detail — BHK/add-on picker | Suggest a likely BHK size + add-ons from a description like "quiet 2BHK under ₹25,000" | Planned, not built |
| `FactsBoard` (Travel and Property) | Turn a Java-computed number into a plain sentence — this one's the *simplest* AI use case, worth building first | Planned, not built |
| Dashboard | The provider status widget from the diagram below | Planned, not built |

Nothing in this row is built yet — this table exists so "AI" stops being an abstract future thing and becomes a specific list of screens, each with a specific, small job.

### The concrete scenario, walked through step by step

You research a route yourself (Mumbai → Kerala, checked against Google) and would otherwise hand-type each leg into the `RouteComparison` screen you already have. Here's every step of what happens instead, in order:

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'basis'}}}%%
flowchart TB
    A(["1. You type or paste your plan<br/>in plain English"]) --> B["2. Frontend sends that text<br/>to the Travel backend"]
    B --> C{{"3. AiProviderRouter picks a<br/>provider — Ollama first"}}
    C --> D["4. Spring AI's ChatClient sends<br/>your text + a strict schema"]
    D --> E["5. The model replies with<br/>structured data, not prose"]
    E --> F["6. Saved as AiSuggestion,<br/>status = DRAFT"]
    F --> G(["7. You see it pre-filled in<br/>the same RouteComparison screen"])
    G --> H{{"8. You edit anything wrong,<br/>then Accept or Reject"}}
    H -->|Accept| I["9. Deterministic Java turns it into<br/>real trip_leg rows — not an AI step"]
    H -->|Reject| J(["Your original typed plan,<br/>completely untouched"])

    class A,G,J start;
    class C,H decision;
    class D,E,F ai;
    class I real;

    classDef start fill:#334155,stroke:#94a3b8,color:#f1f5f9,stroke-width:2px,rx:10,ry:10;
    classDef decision fill:#4338ca,stroke:#c7d2fe,color:#eef2ff,stroke-width:2px,rx:8,ry:8;
    classDef ai fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,rx:8,ry:8;
    classDef real fill:#15803d,stroke:#86efac,color:#dcfce7,stroke-width:2px,rx:8,ry:8;
```

**What each numbered step actually means, in plain terms:**

1. **You type or paste your plan.** Something like *"Mumbai to Kochi by flight, then Kochi to Munnar by road, back the same way."* No special format needed — this is the whole point of using an LLM here, it's good at understanding ordinary sentences.
2. **The frontend sends that text to the backend.** Just a normal API call, like every other request this app already makes — nothing AI-specific about this step.
3. **`AiProviderRouter` picks a provider.** The fallback-chain logic from earlier in this section — tries Ollama (your own computer) first, only reaches for Groq or Mistral if Ollama isn't available.
4. **Spring AI's `ChatClient` sends your text, plus a schema.** This is the "structured output" feature explained in §18.0 — instead of just asking for an answer, the request also says *"reply using exactly this shape: a list of legs, each with a from-city, to-city, and transport mode."*
5. **The model replies with structured data.** Because of that schema, what comes back is something Java can directly turn into objects — city names, leg order, transport modes — not a paragraph you'd have to parse yourself.
6. **It's saved as a draft**, tagged `DRAFT`, with which model produced it and when — the same provenance pattern used everywhere else in this project ([ADR-005](docs/decisions/ADR-005-price-snapshot-strategy.md)).
7. **You see it pre-filled** — literally in the same `RouteComparison` screen that already exists, just with the legs already typed in instead of blank.
8. **You review it.** Edit anything wrong, exactly like editing any other field on that screen already.
9. **Accept or reject.** Accepting is a plain, deterministic Java operation — copying the draft into real `trip_leg` rows — not an AI action. Rejecting leaves everything exactly as you'd typed it yourself; nothing is lost.

This is [docs/ai-capability-roadmap.md](docs/ai-capability-roadmap.md)'s item 1 ("structured trip extraction"), now walked through concretely instead of staying abstract.

### The other places AI genuinely helps here

| Use case | What AI does | What AI never does |
|---|---|---|
| **Structured extraction** (above) | Turns your natural-language plan into draft `trip_leg`/`route_option` rows | Save them without your review |
| **Missing-information detection** | Explains a gap in plain language ("no return leg specified") | Decide the plan is "complete" — that check is deterministic Java against required fields |
| **Estimate drafting when a master is missing** | If no `leg_price_quote`/`rent_snapshot` exists yet, drafts a clearly-labeled "AI-estimated, unverified" placeholder so planning isn't blocked | Ever let that estimate silently pass as a real, sourced price |
| **Anomaly surfacing** | Flags "Aundh has no recorded 4BHK snapshot — worth double-checking" as a *question* | Assert "Aundh has no 4BHK" as fact — absence of a snapshot means *unknown*, not *no*, per [§19](#19-data-validation--honest-limitations) |
| **Document/visa checklist drafting** | Drafts a `document_requirement` checklist from the `visa_requirement` master | Certify it's correct — that master is manually curated and needs its own verification |
| **Price-change narration** | Turns Java-computed differences between `leg_price_quote`/`rent_snapshot` rows into plain language | Compute the difference itself |
| **RAG over your own saved plans** (later) | Lets you ask questions across your own trips/plans once there are enough of them to search | Answer from anything other than your own retrieved data |

### Smart search / autocomplete — the "type Dubai, get suggestions" box

**Frontend built.** `lifestyle-web/src/lib/placeSearch.ts` (the DB-first search function) and `components/PlaceAutocomplete.tsx` (the generic input+dropdown), wired into both `TravelFolderPage` (searches place names already saved inside trip legs; picking one filters the trip folders shown) and `CityFolderPage` (searches city/area/locality, disambiguated by parent context; picking one navigates straight to that page) — one component, two call sites, per [§10](#10-non-negotiable-engineering-rules). Today it searches this app's local static data (`tripData.ts`/`cityData.ts`/`areaData.ts`), standing in for what will be real Postgres master queries once the backend exists — the search *shape* doesn't change when that swap happens, only where the rows come from. There is no AI call yet: when nothing local matches, the dropdown shows an honest "no local match — AI would appear here once a backend + provider are wired up" notice (`AI_FALLBACK_NOTICE`) instead of faking one.

**Backend built too, same contract, still hexagonal — DB-first search stays in `travel-service`/`property-service`, every external call moved into a third microservice.** Both `travel-service` and `property-service` have a `PlaceSearchUseCase` (application layer), a `PlaceRepository` outbound port (`application/port/out`), an `InMemoryPlaceRepository` adapter (`adapter/out/persistence`) with the same illustrative places as the frontend, and a `PlaceSearchController` exposing `GET /api/{travel|property}/v1/places/search?q=`. The DB-first order is unchanged: local repository first, always; when it finds nothing, the response still carries only the honest `AI_FALLBACK_NOTICE` string — neither service touches the network itself.

### Where the external API calls actually live: `integration-service`

An earlier pass in this same session put `GroqAiProvider`/`MistralAiProvider`/`AiProviderRouter`/`GeoNamesClient`/`DataGovInClient` directly inside `travel-service` and `property-service`. On reflection this meant duplicating every provider client and every Resilience4j wrapper in two places for no reason — neither service's actual job is "talk to Groq." That code was reverted, and a **third microservice, `integration-service`**, was built to hold it instead — same Gradle/Spring Boot conventions, same `domain`/`application`/`adapter`/`config` hexagonal layering as the other two, running on its own port (`8083`, `INTEGRATION_PORT`). Its role is narrower than the other two: it owns no *business* schema like Travel's or Property's — no `trip_plan`, no `property_plan` — its only job is holding every outbound call to an external system behind a small set of ports and adapters. It does, since a parallel-machine session's update, have one small table of its own (`integration.ai_suggestion`, below) purely to track AI-draft provenance, not domain data.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'stepAfter'}}}%%
flowchart LR
    T["travel-service"]
    P["property-service"]
    I["integration-service"]
    O(["Ollama"])
    G(["Groq"])
    M(["Mistral"])
    GN(["GeoNames"])
    F(["Frankfurter"])
    D(["data.gov.in"])

    T -->|"master refresh — live"| I
    P -->|"master refresh — live"| I
    T -.->|"AI fallback — not yet wired"| I
    P -.->|"AI fallback — not yet wired"| I
    I --> O
    I --> G
    I --> M
    I --> GN
    I --> F
    I -.->|"manual endpoint only"| D

    classDef svc fill:#334155,stroke:#94a3b8,color:#f1f5f9,stroke-width:2px,rx:10,ry:10;
    classDef ext fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,rx:10,ry:10;
    class T,P,I svc;
    class O,G,M,GN,F,D ext;
```

What `integration-service` contains today:

- **`AiProviderRouter`** (`adapter/out/ai`) tries **Ollama, then Groq, then Mistral** — Ollama first since it's local/free/private, the two cloud providers as fallback (`OllamaAiProvider`/`GroqAiProvider`/`MistralAiProvider`, the cloud two calling their real OpenAI-compatible chat-completions endpoint via `RestClient`), wrapped per-provider in a hand-constructed Resilience4j `RateLimiter` + `CircuitBreaker` (core library, not the Spring Boot starter module — its Boot-4.1 compatibility couldn't be verified here), skipping any provider with no configuration and falling through to the next on any failure. Returns a `RoutedSuggestion(suggestions, provider, model)` record, not a bare list — so callers know *which* provider actually answered.
- **`PlaceSuggestionUseCase`** wraps the router as the application-layer entry point, exposed via `GET /api/integration/v1/ai/place-suggestions?q=`. Since the parallel-machine update, every successful suggestion is also persisted — a real Postgres write via `JdbcAiSuggestionRepository` into `integration.ai_suggestion` (`V1__ai_suggestion.sql`), the same `provider`/`model`/`prompt_version`/`verification_status`/`acceptance_status` DRAFT-lifecycle shape as `travel.ai_suggestion`/`property.ai_recommendation` in §16 — this is real, working AI-draft provenance tracking, not a placeholder. Results always carry an explicit "unverified" disclaimer (§10, AI never authoritative) regardless.
- **`GeoNamesClient`** — genuinely live now, not just a manual endpoint: `travel-service`/`property-service`'s `MasterRefreshUseCase` (§20) calls it through `GET /api/integration/v1/masters/geonames/search?q=` to refresh their own `city` master.
- **`FrankfurterClient`** (new, no key needed) — same story: called live by both services' `MasterRefreshUseCase` through `GET /api/integration/v1/masters/frankfurter/latest?base=&symbols=` to refresh their `currency` master.
- **`DataGovInClient`** — still manual-trigger only (`GET /api/integration/v1/masters/datagovin/sample`); its dataset's exact response shape has never been inspected against a real key/response, so no refresh job consumes it yet (§20).
- **`GET /api/integration/v1/ai/providers/status`** and **`GET /api/integration/v1/masters/health`** report each provider/client's `configured` flag *and* real health — see below.

### Rate limiting, health, and accuracy — updated 2026-09-16 with real researched numbers

**Every outbound call in `integration-service` — AI or not — now goes through one shared component, `ResilienceGuard`** (`adapter/out/resilience`), instead of `AiProviderRouter` hand-rolling its own `RateLimiter`/`CircuitBreaker` maps while `GeoNamesClient`/`FrankfurterClient`/`DataGovInClient` had none at all (a real gap this pass closed — those three previously had zero rate limiting). `ResilienceGuard.call(name, config, action, fallback)` wraps any call with a named Resilience4j `RateLimiter` + `CircuitBreaker` pair and, as a side effect of every call, updates a `HealthStatusRegistry` entry for that name.

**Every limit below is that provider's own published number, researched 2026-09-16, with a safety margin — except one, flagged honestly:**

| Provider / client | Published limit (source) | This app's cap |
|---|---|---|
| GeoNames | 1,000 credits/hour, 10,000/day ([geonames.org/export/credits.html](https://www.geonames.org/export/credits.html)) | 10 req/min (≈600/hr) |
| Groq | 30 requests/minute, org-wide, per model | 25 req/min |
| Mistral | **1 request/second**, org-wide (Mistral's own help center) | 1 req per 1.2s — deliberately *not* expressed as "60/min," since a per-minute bucket would let a burst blow straight past a real per-second cap in the first second of every window |
| Frankfurter | No published quota — its own docs say "soft fair-use limits, no monthly/daily caps" | 20 req/min (a self-imposed courtesy cap, not something Frankfurter enforces) |
| data.gov.in | **No numeric limit published anywhere found**, after a real search — flagged as genuinely unverified, not guessed with false confidence | 10 req/min (a conservative placeholder; tighten or loosen once a real number surfaces) |
| Ollama | None — local, concurrency-bound not request-bound | 60 req/min (pure safety valve against a runaway loop, not a real external constraint) |

**Health is decided by the last real hit, updated by the next one — never a synthetic ping**, exactly as designed: `ResilienceGuard` records `UP` on any successful call and `DOWN` (with a reason — rate limited / circuit open / the actual exception message) on any failure, for whichever provider or client just got called. `HealthStatusRegistry` is a simple in-memory map, per process, reset on restart — that's the right scope for "is this working right now," not a persisted audit trail (`integration.ai_suggestion`, above, is what actually persists). The status endpoints just read this map; they never themselves call an external API, so checking status is always free and instant, regardless of whether a provider is actually reachable at that moment. The Dashboard's AI-status widget (§15.4) consumes exactly this: green = `UP`, red = `DOWN`, orange = not configured or never called yet.

**Sequential fallback stays the default; parallel cross-checking is available but deliberately unused by default.** `AiProviderRouter.suggestPlaces()` — the only method `PlaceSuggestionUseCase` actually calls — remains strictly sequential (Ollama → Groq → Mistral, stop at the first success), because that's the only mode compatible with the tight, real limits in the table above: calling all three in parallel for every ordinary place-name suggestion would burn 3× the quota for a low-stakes answer. A second method, `AiProviderRouter.suggestPlacesVerified()`, calls **Ollama and Groq in parallel** (Mistral deliberately excluded — its 1/sec ceiling is the tightest of the three, not worth spending on routine cross-checks) and reports whether they agree. This exists and is tested but **is not called by anything yet** — it's held in reserve for exactly the case §18's own research above already identified as the one place parallel verification is worth its cost: a future high-stakes structured-extraction draft (phased-plan item 7), never routine suggestions. This is the concrete decision asked for: **priority order (sequential) for everything today; parallel only for a specific future high-stakes feature, not as a general default.**

**Partially wired now, not fully.** As of the Settings/master-refresh feature (§20), `travel-service`/`property-service` **do** call `integration-service` over HTTP — for master-data refresh (GeoNames, Frankfurter), via a new `IntegrationServiceClient` in each service. What's still **not** wired: `PlaceSearchUseCase`'s AI fallback still only returns the honest `AI_FALLBACK_NOTICE` string — it does not yet call `PlaceSuggestionController` for a real AI-drafted suggestion when local search finds nothing. That specific call remains the next step. `InMemoryPlaceRepository` (backing place *search*, not the *master* table `MasterRefreshUseCase` now actually populates — see §20's note on that gap) and the frontend's `placeSearch.ts` are hand-kept in sync (same city/area/locality/trip-leg fixture values) since there's no shared package between a Java backend and a TypeScript frontend to enforce it automatically. **Not compiled or run** on this machine — Java 8 here (plus an unrelated Java 17 install at `D:\jdk-17.0.6`), neither satisfying the project's Java 25 toolchain requirement; `./gradlew compileJava --offline` fails cleanly on all three services asking for a JDK 25 toolchain rather than downloading one, reconfirmed after every pass this session including the master-refresh work, with no dependency-resolution errors beyond that gap. Verify by building on a machine with JDK 25 before trusting any of this compiles. The "via" route-suggestion drafting (a still-separate idea from ad-hoc place suggestions) remains design-only.

This is a real, well-scoped feature, and it maps cleanly onto everything already designed: a search box for Travel's from/to fields and Property's location search, checking **the database first, AI only when nothing local matches.**

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'basis'}}}%%
flowchart TB
    A(["You type: 'Dubai'"]) --> B["PlaceSearchService"]
    B --> C{{"1. Search local masters first —<br/>city / area / locality / your own past routes"}}
    C -->|found| D["2a. Return instantly —<br/>free, no AI call, no rate limit spent"]
    C -->|nothing found| E["2b. Only now, ask AI to suggest matches<br/>— clearly marked unverified"]
    D --> F(["3. Show suggestions,<br/>'via' options if planning a route"])
    E --> F
    F --> G(["4. You pick one — click fills<br/>the field, keep typing to refine"])

    class A,F,G start;
    class C decision;
    class D real;
    class E ai;

    classDef start fill:#334155,stroke:#94a3b8,color:#f1f5f9,stroke-width:2px,rx:10,ry:10;
    classDef decision fill:#4338ca,stroke:#c7d2fe,color:#eef2ff,stroke-width:2px,rx:8,ry:8;
    classDef real fill:#15803d,stroke:#86efac,color:#dcfce7,stroke-width:2px,rx:8,ry:8;
    classDef ai fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,rx:8,ry:8;
```

**One generic service, not two.** `PlaceSearchService` (backend) + a matching `PlaceAutocomplete` React component (frontend) — used by *both* Travel's from/to fields and Property's location search, per the [generic-over-bespoke rule](#10-non-negotiable-engineering-rules). The only thing that differs is which master tables it searches — Travel searches `city`; Property searches `city`, `area`, `locality`, `pincode` too, since Property's whole point is going deeper than just the city level.

**Why database-first, not AI-first — this was the right instinct:**

1. **Every place already in GeoNames or already saved is free and instant** — a plain SQL prefix search, no AI call, no rate limit spent, no waiting on a network round-trip to Groq or Mistral. Most searches (Pune, Mumbai, Hyderabad, anywhere you've planned before) will hit this path.
2. **AI is only asked when nothing local matches at all** — a genuinely new or unusual place your masters don't have yet. That's the fallback, not the default — exactly the same "AI only when the deterministic path can't answer" rule already used for price estimates and anomaly-surfacing above.
3. **This also directly protects the free-tier rate limits from [§17.3](#173-what-happens-when-a-refresh-call-fails)** — if every keystroke triggered an AI call, three free tiers would be exhausted in minutes just from typing. DB-first search means AI is called rarely, only for genuinely unmatched queries.

**The hierarchy — already exists, just needs using.** GeoNames itself already stores country → state/admin-division → city (that's *why* it's the chosen master source in [§17](#17-master-data-lifecycle)), and Property's own [§16.2](#162-property-schema) already models city → area → locality. Typing "India" and getting states, or "Pune" and jumping straight past that level, is just querying this hierarchy at different depths — no new data model needed, only a search endpoint that knows how to query it.

**Disambiguation** ("Gandhi Road" existing in Pune, Mumbai, and Jaipur) is plain, deterministic result formatting — every match gets its parent context appended (`Gandhi Road, Pune` / `Gandhi Road, Mumbai` / `Gandhi Road, Jaipur`), not an AI decision about which one you meant.

**The "via via" route suggestions — where AI genuinely earns its place:**

- If you've planned a similar route before, that's a database lookup — your own saved `trip_leg` history, not AI.
- For a route you've *never* planned, AI can draft plausible via-city suggestions (e.g., "via Mumbai," "via Delhi" for a Pune→Dubai search) based on general geographic knowledge. This is the same DRAFT pattern as everywhere else — **clearly marked unverified**, since (per [§17](#17-master-data-lifecycle)'s honest gap) there's no free live source confirming a route is actually bookable. AI is genuinely useful here for *plausible starting points*, not for asserting a route exists.
- **Kept to a sensible cap** — one or two via-stops suggested, not an unbounded list, matching what was asked: meaningful options, not noise.

### AI APIs — what's actually free right now, researched not assumed

[Ollama](https://ollama.com/) running a local open-weight model stays this project's default — zero cost, no account, no rate limit tied to someone else's quota, no data leaving your machine ([docs/local-resource-profile.md](docs/local-resource-profile.md) has sizing guidance). That default doesn't change here. But since "we're in the AI era, we'll be using AI APIs" was the direct ask, here's what genuinely has a standing free tier as of 2026, for context:

| Provider | Free tier (as reported, 2026) | Worth knowing |
|---|---|---|
| Google Gemini API | Current Flash-tier models free while billing stays off the account | No card required to start; the moment billing is enabled, pricing applies |
| Groq | ~30 requests/min, ~1,000/day on its free-tier model | Very fast inference; limits are per-model, not account-wide |
| OpenRouter | ~14 free models, up to 50 requests/day | Aggregates many providers behind one API |
| Mistral AI | A standing free mode, no card required | European provider, open-weight-friendly |
| Cloudflare Workers AI | 10,000 "Neurons" (its own compute-credit unit) per day | Ties you to Cloudflare's platform if used beyond the free tier |

Every one of these is sized for development/testing, not production traffic, and every one requires creating an external account — which is exactly the kind of thing that needs your explicit, separate approval before it's wired in, per [AGENTS.md](AGENTS.md). None of them are used by anything in this app today; this table exists so that decision is informed if you ever want to make it, not to quietly justify adding one.

### The AI *framework* layer — what enterprise Spring Boot/React teams actually reach for (researched, 2026)

The tables above cover which AI *provider* to call. This is the different question: what library sits between this app's Java/React code and that provider, so nobody's hand-rolling raw HTTP calls and JSON parsing for every AI feature.

**Backend — Spring AI is now a genuine fit, not just deferred.** [ADR-004](docs/decisions/ADR-004-ai-provider-abstraction.md) deferred Spring AI because there was no call site yet; that's still true, but a version blocker that existed then is gone now. **Spring AI 2.0 went GA in mid-2026**, requires **Spring Boot 4.0/4.1 and Java 21+** — this project already runs Spring Boot 4.1.1 and Java 25, an exact match, not a future upgrade to plan around. What it gives, concretely useful for this project's own patterns:

| Spring AI 2.0 feature | Where it fits this app |
|---|---|
| `ChatClient` + **structured output** — typed Java objects back from the model, not raw text to parse | Exactly the `AiSuggestion`/`AiRecommendation` DRAFT payload shape already designed in [§16](#16-planned-backend-data-model--masters-transactions-audit) — the model fills a Java record, not a string |
| Auto-registered **tool calling** (`ToolCallingAdvisor`) | The "narrowly scoped read-only tools" already planned in [docs/ai-capability-roadmap.md](docs/ai-capability-roadmap.md) item 3 |
| Native **MCP** client + server support (`spring-ai-starter-mcp-client`) | [Model Context Protocol](https://modelcontextprotocol.io/) is becoming the standard way an AI model calls back into an app's own tools/data — worth knowing about now even though nothing here uses it yet; it's the same tool-calling idea above, just over a standardized protocol instead of a bespoke one |
| RAG pipeline building blocks | [docs/ai-capability-roadmap.md](docs/ai-capability-roadmap.md) item 6, still gated on the same "only if PostgreSQL full-text search isn't enough" condition already documented |

**The alternative, and why Spring AI wins here specifically:** [LangChain4j](https://docs.langchain4j.dev/) is the other serious Java option — framework-agnostic (works with Quarkus, Micronaut, plain Java too), broader provider/vector-store coverage, faster release cadence but more breaking changes between versions. For a project that's already committed to Spring Boot everywhere else — Actuator health checks, dependency injection, the whole [hexagonal adapter pattern](#6-inside-one-backend-service) — Spring AI's native fit (its `ChatClient` is just another outbound adapter behind the existing `AiProvider` port, no new wiring philosophy) is the better match. LangChain4j would be the right call for a project *not* already all-in on Spring; that's not this one.

**Frontend — deliberately not adopting the Vercel AI SDK.** It's the genuine React-ecosystem standard for AI chat UIs (`useChat`, streaming, provider-agnostic), but its whole design assumes the AI call happens in a Node.js/Next.js server function calling the provider directly. This project's rule is stricter: **AI only ever runs behind the Spring Boot backend's `AiProvider` port, never client-side, never from the browser** — that boundary is what makes provenance, approval, and the DRAFT/ACCEPTED pattern enforceable at all. Pulling in a library built around a different backend shape would mean fighting it, not using it. If a future feature needs streaming AI output in the UI, the right move is a small custom React hook reading a Server-Sent-Events endpoint from *our own* backend — the same UX pattern `useChat` provides, without adopting an SDK built for someone else's architecture.

**So where does this project actually use Spring AI? Nowhere — direct answer, since this comes up.** Everything above was written as a *recommendation* before `integration-service` existed. When `GroqAiProvider`/`MistralAiProvider`/`OllamaAiProvider`/`AiProviderRouter` were actually built (§18 below), **plain Spring `RestClient` calls were used instead — no `spring-ai-*` dependency is in any `build.gradle` in this repo.** This wasn't a silent reversal of the recommendation above; it's a scope call: Groq, Mistral, and Ollama's chat-completions APIs are all OpenAI-compatible JSON-over-HTTP — one POST, one prompt string, parse a comma-separated reply — genuinely simple enough that hand-writing the `RestClient` call was faster to get right and easier to verify by reading, especially given every line of this project's backend has had to be verified by eye rather than compiled (§12). Adding Spring AI's `ChatClient` + provider-specific starter dependencies would have meant more unverified dependency-resolution risk for a call this simple. **The recommendation above still stands as the honest answer for when it *would* earn its place**: structured output (typed Java objects instead of parsing a comma-separated string by hand) becomes worth it the moment a real feature needs the model to fill a whole `AiSuggestion`-shaped record (§16) instead of a flat list of strings — i.e. item 7 in the [phased plan](#21-phased-plan), not before.

### Multiple AI providers — the full architecture

Ollama (local), Groq, and Mistral are all set up now ([§22](#22-account-setup-checklist--whats-needed-from-you-one-at-a-time)). Here's how they fit together, not just as three separate options but as one system:

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background':'transparent','fontFamily':'Trebuchet MS, Verdana, sans-serif','fontSize':'15px','lineColor':'#5eead4'}, 'flowchart': {'curve': 'stepAfter'}}}%%
flowchart TB
    UseCase["Use case<br/>e.g. draft a trip"] --> Router{{"AiProviderRouter<br/>tries in priority order"}}

    subgraph Resilience["Resilience4j — per provider"]
        direction LR
        CB["Circuit breaker"]
        RL["Rate limiter"]
        RT["Retry"]
    end

    Router --> Resilience
    Resilience --> Ollama["Ollama<br/>local, tried first"]
    Resilience -.->|"if Ollama unavailable"| Groq["Groq<br/>cloud fallback"]
    Resilience -.->|"if Groq rate-limited"| Mistral["Mistral<br/>cloud fallback"]

    Ollama --> Draft["AiSuggestion — DRAFT"]
    Groq --> Draft
    Mistral --> Draft
    Draft --> Human["Human reviews and accepts"]

    Router -.-> Health["HealthIndicator<br/>per provider"]
    Health --> StatusAPI["GET /ai/providers/status"]
    StatusAPI --> Widget["Dashboard widget<br/>+ Refresh button"]

    class Ollama local;
    class Groq,Mistral cloud;
    class Draft,Human result;
    class Widget,StatusAPI frontend;

    classDef local fill:#15803d,stroke:#86efac,color:#dcfce7,stroke-width:2px,rx:8,ry:8;
    classDef cloud fill:#b45309,stroke:#fde68a,color:#fffbeb,stroke-width:2px,stroke-dasharray:4 4,rx:8,ry:8;
    classDef result fill:#4338ca,stroke:#c7d2fe,color:#eef2ff,stroke-width:2px,rx:8,ry:8;
    classDef frontend fill:#164e63,stroke:#67e8f9,color:#ecfeff,stroke-width:2px,rx:8,ry:8;
```

**How the pieces work, each one researched, not assumed:**

- **Fallback chain, not simultaneous calls.** `AiProviderRouter` tries Ollama first (free, private, no external rate limit), then Groq, then Mistral, only moving to the next when the current one fails or its circuit is open — this is the documented [Spring Retry cascading-fallback pattern](https://www.baeldung.com/spring-ai-configure-multiple-llms) for Spring AI, not something bespoke.
- **Rate limiting is per-provider, reusing [§17.3](#173-what-happens-when-a-refresh-call-fails)'s Resilience4j pattern exactly** — a `RateLimiter` configured to each provider's actual documented free-tier ceiling (Groq's ~30 req/min, Mistral's ~1 req/sec), so this app never exceeds a quota and gets itself blocked. Same library, same reasoning, now applied to AI calls instead of master-data refreshes — the generic-over-bespoke rule in action.
- **The status widget is a Spring Boot Actuator pattern, not a custom mechanism:** one small `HealthIndicator` per provider (a cheap ping/test call, not a real generation request), aggregated the same way Actuator already aggregates `/actuator/health` — the Dashboard widget just polls a small `/ai/providers/status` endpoint built on that. "Refresh" re-runs the checks on demand, the same on-demand/scheduled pattern as master refreshing in [§17](#17-master-data-lifecycle). Kept deliberately light on the Dashboard, per its own one-viewport rule ([§15.4](#15-functionality-chapters)) — a compact row of provider name + status dot, not a full panel.

**Built vs. this diagram, precisely — updated again 2026-09-16:** the fallback order (Ollama → Groq → Mistral) is real. Rate limiting/circuit breaking are real but no longer hand-rolled per-class — both now go through the shared `ResilienceGuard` described just above, with per-provider limits sourced from each provider's own published number (see the table there), not the rounded "~30/min, ~1/sec" figures this diagram used when first drawn. One thing in this diagram remains aspirational: there's still no separate `Retry` component (just the rate limiter + circuit breaker — retrying a rate-limited call immediately would just get rate-limited again). The status endpoint is **no longer** a config-only check as an earlier version of this note said: `GET /api/integration/v1/ai/providers/status` now also reports real health (`ResilienceGuard`-recorded, from the last actual call — never a fresh `HealthIndicator`-style ping, which would cost quota just to render a dot), and the Dashboard widget (§15.4) does consume it now, rendering a green/red/orange dot per provider.

### Does using multiple AIs actually mean more accuracy? — corrected, with sources

Not automatically — worth being precise about what it does and doesn't buy:

- **What multiple providers genuinely buys: resilience, not accuracy.** If Ollama isn't running or Groq's free tier is exhausted for the day, the app keeps working instead of failing outright. That's real and valuable for a system with three free-but-imperfect providers — but it's *availability*, not a better answer.
- **What genuinely does improve accuracy: deliberately calling more than one provider for the *same* request and comparing answers** — this is a real, researched technique (LLM ensembling — majority voting, confidence scoring, flagging disagreement), and recent research on it is substantial. But it costs real multiples: two providers called for one draft means twice the calls (and twice the rate-limit budget spent) for that one answer.
- **Where it's worth doing here, specifically:** only for the highest-stakes draft — structured trip/route extraction ([§18](#18-ais-role-in-this-application)'s Mumbai→Kerala scenario) — call two providers, and if they extract different cities/legs, flag that specific field as "models disagreed, please double-check" in the review UI, rather than silently picking one. That's a targeted use of ensembling where the cost is worth it, not a blanket "always call every provider" policy that would burn through three separate free-tier quotas for every single AI action in the app.

So: you weren't wrong that it can help, just that it's not automatic — it has to be a deliberate design choice on a specific, worthwhile task, not "more providers configured = more accurate by default."

**This is now a real method, not just a paragraph.** `AiProviderRouter.suggestPlacesVerified()` (2026-09-16) calls Ollama and Groq in parallel and reports whether they agree — built exactly per the reasoning above, and exactly as unused by default: `PlaceSuggestionUseCase` still calls the plain sequential `suggestPlaces()`. It's there, tested, ready for whichever future feature is the first genuinely high-stakes draft — not switched on generally.

---

## 19. Data validation — honest limitations

"200% accuracy" isn't achievable with free data sources, and pretending otherwise would be worse than saying so plainly. What's actually achievable:

- **Deterministic validation, not AI validation.** Whether a `(locality, bhk_type)` pair has ever had a real `rent_snapshot`, whether a `trip_leg`'s transport mode has a matching schedule entry — these are database lookups in Java, the same "AI never computes facts" rule already governing money math ([§10](#10-non-negotiable-engineering-rules)).
- **Three states, not two.** For anything sourced from limited free data, the honest answer to "does this exist" is **yes / no / unknown** — not a forced yes/no. No snapshot for Airwada 4BHK means *no data yet*, not *confirmed unavailable*. The UI should say which one it means.
- **Provenance on everything**, already the pattern from [ADR-005](docs/decisions/ADR-005-price-snapshot-strategy.md) and [docs/data-strategy.md](docs/data-strategy.md): every master row and every snapshot carries source, captured_at, and confidence, so a stale or manually-guessed value is visibly different from a verified one.
- **What free data genuinely cannot give this app**, stated plainly: live train/flight schedules and fares, and real per-locality rental inventory. Both require either a paid API (needs your explicit approval per [AGENTS.md](AGENTS.md)) or manual/crowdsourced entry. This project's answer, consistent with everything already documented ([docs/free-data-sources.md](docs/free-data-sources.md)): start manual, stay honest about it, revisit only if a genuinely free or approved source appears.

---

## 20. Settings (frontend)

A new `/settings` page, kept deliberately small — no account/profile/name fields, matching "we don't need any of that yet":

| Setting | What it does | Backed by |
|---|---|---|
| Currency | Switches the display currency app-wide (fares, rents, totals) | `currency` master's exchange rates ([§16](#16-planned-backend-data-model--masters-transactions-audit)); a preference stored the same lightweight way as [§15.4](#15-functionality-chapters)'s `lastAccessed` — no account needed to remember it |
| Refresh masters | Triggers the on-demand path from [§17](#17-master-data-lifecycle) and shows each master's last-refreshed time/status | The `master_refresh_log` table |

Nothing else belongs here yet — this list grows only when a real, working setting needs a home, not speculatively.

**Built, 2026-09-16 — real code across all four apps, none of it hardcoded, none of it verified to run on this machine.** `lifestyle-web/src/features/settings/SettingsPage.tsx` (route `/settings`, linked from `Nav.tsx`) renders two things, both dynamic:

- **Currency selector** — options come from `GET /api/{travel|property}/v1/masters/currencies`, i.e. whatever's actually in the `currency` table right now; the page renders nothing hardcoded and shows an honest "no currencies loaded yet" state if that table is empty. Selection is saved via `lib/currencyPreference.ts` (localStorage, same lightweight pattern as `lastAccessed.ts` — a browser preference, not an account setting, matching this page's "no accounts" rule).
- **Master data + Refresh button** — `GET /api/{travel|property}/v1/masters/refresh-status` renders each master's live record count and last-refresh time/status straight from Postgres (`master_refresh_log`, added in a new `V3__master_refresh_log.sql` migration in both services — additive, doesn't touch the already-applied V1/V2). The button calls `POST /api/{travel|property}/v1/masters/refresh` on both services, which runs `MasterRefreshUseCase`: pulls the `city` master from GeoNames and the `currency` master from Frankfurter — both **through `integration-service`**, via a new `IntegrationServiceClient` in each domain service, never called directly — and upserts each into Postgres via `INSERT ... ON CONFLICT DO UPDATE` (a natural-key uniqueness constraint on `city(name, country_code)` was added in the same V3 migration; `currency.iso_code` already had one). Every refresh attempt is logged even on failure (§17.3) — nothing here silently does nothing. `integration-service` gained one new external integration for this: `FrankfurterClient` (genuinely free, no key — §22.1), exposed at `GET /api/integration/v1/masters/frankfurter/latest?base=&symbols=`.

**Expanded 2026-09-17 — `transport_mode` (travel), `bhk_type` and `service_addon` (property) now also refresh, with zero network dependency.** These are a genuinely different kind of master from `city`/`currency`: fixed, closed sets of categorical labels (flight/train/road; 1RK–Villa; tiffin/gym) with no external source to fetch from at all — there's nothing to look up, so `MasterRefreshUseCase` seeds them directly and always reports `SUCCESS`, since no network call is involved. This means these three now populate correctly on every refresh **regardless of whatever's happening with `integration-service`'s reachability** — a real, working improvement independent of the Docker networking bug being chased in the decision log below. The Settings page needed **zero frontend changes** to show them: it already renders whatever `GET /masters/refresh-status` returns, generically, with no hardcoded list of master names.

**Still not refreshed, and why — this remains a deliberate scope boundary, not an oversight.** Property's `area`/`locality`/`pincode` masters are designed to come from data.gov.in's pincode-directory dataset (§17, §22.2) but are **not** wired into this refresh — that dataset's exact JSON response shape has never been inspected against a real key/response, and guessing it wrong risks silently mis-mapping the area→locality→pincode hierarchy with no way to catch it. `visa_requirement` and `municipality` still have no seed either: visa rules are asserted facts (inventing placeholder ones would violate §10's "never assert an unverified fact" rule, unlike a plain categorical label like "Flight"), and `municipality` rows need a `city_id` foreign key, adding a real ordering dependency (city must be upserted and looked up first) that hasn't been built yet. **"Transactions get pulled on first refresh too"** was part of the original ask — honestly, there's nothing to pull yet: no CRUD UI or backend exists for `trip_plan`/`route_option`/`property_plan` (§21's phased plan puts that after masters), so a transaction-refresh has no real data to operate on until that's built.

**Not verified end to end.** Every backend file above is written but not compiled — same Java-25-toolchain gap as everything else this session (`./gradlew compileJava --offline` reconfirmed clean on all three services after these changes). The frontend half **is** verified — `npm run lint`, `npm test`, `npm run build` all pass, and the `/settings` route serves correctly from the dev server — but only against a backend that isn't running here, so its honest connection-error states (rather than the real success path) are what's actually been exercised. Also added: `spring.datasource`/`jpa`/`flyway` were already enabled in `travel-service`/`property-service` `application.yaml` by a parallel session on another machine (real Postgres schemas `travel`/`property`/`integration` already exist there) before this feature was built — this feature was written against that real schema (the exact column definitions in `V2__domain_tables.sql`), not guessed.

---

## 21. Phased plan

Not a commitment, not scheduled — an honest sequencing of everything above, so the next real decision has an order to slot into. See [docs/roadmap.md](docs/roadmap.md) for what's officially planned next; this is the more detailed version of the same idea for backend/AI work specifically.

1. **Masters first, minimal set.** `country`, `city`, `currency` (Travel + Property, each schema's own copy) — pull once from GeoNames/REST Countries/Frankfurter, verify the refresh mechanism works before building anything on top of it. **Partially done (2026-09-16, §20):** `city` + `currency` refresh built and on-demand-triggerable in both services via GeoNames/Frankfurter — not yet runtime-verified (compile gap, §12), and `country`/REST Countries isn't wired at all yet.
2. **Transaction tables, matching the frontend's existing shape.** `trip_plan`/`route_option`/`trip_leg` and `property_plan`/`cost_estimate` — deliberately designed to match `tripData.ts`/`cityData.ts`/`areaData.ts` already, so the frontend swaps its data source rather than being rebuilt. **Schema exists** (Flyway V2, §16) — no application code reads/writes any of these tables yet.
3. **Audit logging from day one of transaction tables** — cheaper to build in from the start than retrofit. `audit_log` tables exist in the schema; nothing writes to them yet since no transaction code exists yet either.
4. **Manual price/rent entry UI**, since no free live source exists for either — a form backed by `leg_price_quote`/`rent_snapshot`, append-only. Not started.
5. **Master refresh scheduling** (`@Scheduled`) once the on-demand path is proven. The on-demand path now exists (§20) but hasn't run in production yet — scheduling remains a later step, correctly, per this list's own order.
6. **Settings page**, once there's at least one real setting (currency) and one real trigger (refresh) to back it. **Done (2026-09-16, §20)** — both conditions this step was waiting on are now met.
7. **AI structured extraction** (the Mumbai→Kerala scenario) — the first AI feature, once there's a real `trip_leg` shape to draft into and a review/accept flow to draft against. Not started — the AI *provider* plumbing this needs already exists (§18), but `trip_leg` has no application code yet (item 2), so there's nothing to extract into.
8. **The rest of [docs/ai-capability-roadmap.md](docs/ai-capability-roadmap.md)'s sequence** — missing-field detection, tool calling, explanations, RAG — each only after the step before it is genuinely working, per that document's own rule.

---

## 22. Account setup checklist — what's needed from you, one at a time

Everything below is free, verified just now (not assumed), and filtered against one hard rule you set: **no time-limited trials, nothing that starts free and converts to paid.** Where a provider's own 2026 policy made that unclear, it's flagged and skipped rather than guessed at.

**Where credentials go:** your own `LOCAL-ACCESS.md`, outside this repo — [AGENTS.md](AGENTS.md) already says never to copy it into the repo or have it read without a stated need. I never need to see the actual key values for anything — every one of these integrations lives in **`integration-service`** (see §18's "Where the external API calls actually live"), which reads each credential from an environment variable, with `application.yaml` holding only `${ENV_VAR:}` placeholders, the same pattern `README.md` already used for the database passwords. **To actually run `integration-service` with these integrations live, export these in your own shell before `bootRun`** (values from your `LOCAL-ACCESS.md`, never typed anywhere I can see them):

| Environment variable | Used by | Required? |
|---|---|---|
| `GEONAMES_USERNAME` | `GeoNamesClient` (`integration-service`) | Optional — unset means that client returns no results, nothing crashes |
| `DATA_GOV_IN_API_KEY` | `DataGovInClient` (`integration-service`) | Optional, same behavior |
| `DATA_GOV_IN_RESOURCE_ID` | `DataGovInClient` (`integration-service`) | Already defaults to the pincode dataset above — override only for a different dataset |
| `GROQ_API_KEY` | `GroqAiProvider` (`integration-service`) | Optional — unconfigured providers are skipped, not treated as errors |
| `MISTRAL_API_KEY` | `MistralAiProvider` (`integration-service`) | Optional, same behavior |
| `GROQ_MODEL` / `MISTRAL_MODEL` | same providers | Already default to a sensible current model — override only to change it |
| `AI_REQUESTS_PER_MINUTE` | `AiProviderRouter` (`integration-service`) | Already defaults to `20` |

### 22.1 No signup needed — nothing to do

| Source | Used for |
|---|---|
| [Frankfurter](https://frankfurter.dev/) | Currency exchange rates |
| [REST Countries](https://restcountries.com/) | Country metadata |
| [Ollama](https://ollama.com/download) | Local AI — install the app, no account, matches the project's default |

### 22.2 Free accounts to create, one by one

1. **GeoNames** (city/place master data) — [geonames.org/login](https://www.geonames.org/login).
   - Sign up → confirm the email → **then go back to your account page and explicitly enable "free web services"** at the bottom of the profile. A bare signup isn't enough — this extra step is easy to miss and is the single most common reason people think their GeoNames key "doesn't work."
   - Store: the GeoNames **username** (its auth is username-based, not a typical API key).
2. **data.gov.in** (Indian pincode master data) — [data.gov.in](https://www.data.gov.in/), register, then generate your key from "My Account."
   - A shared public sample key exists but throttles hard under any real use — worth registering your own.
   - Store (locally, in `LOCAL-ACCESS.md`): the API key.
   - **Confirmed dataset** (safe to record here — a resource ID isn't a secret): the "All-India Pincode Directory" resource ID is `5c2f62fe-5afa-4119-a499-fec9d604d5bd`, i.e. `https://api.data.gov.in/resource/5c2f62fe-5afa-4119-a499-fec9d604d5bd`. The `api-key` query parameter still comes from your local `LOCAL-ACCESS.md`, never from here.
3. **Groq** (AI inference — fast, genuinely free, no card) — [console.groq.com](https://console.groq.com/).
   - Sign up with email or Google, generate a key immediately. No credits system — rate-limited (roughly 30 req/min, ~14,400/day), not time-limited *as a tier*. **Correction, confirmed from the actual console:** individual keys do carry an expiration date (a security/rotation practice, ~90 days from creation) — that's the key needing to be regenerated periodically, not the free tier itself converting to paid or shutting off.
   - Store: the API key.
4. **Mistral AI** (a second free AI option, different model family) — [console.mistral.ai](https://console.mistral.ai/).
   - Sign up, verify by phone (no card), generate a key from Studio → API Keys. Note: Mistral now prompts for a key **expiration date** on creation — that's a key-rotation habit, not a trial; pick a generous date (e.g. 1 year) and note it so it gets rotated, not because the tier itself expires.
   - Store: the API key + the expiration date you chose.

### 22.3 Skipped for now — flagged, not silently included

- **Google Gemini API** — genuinely free (Flash models) *today*, but Google's own policy shifted in March 2026 to require **new** accounts to set up prepaid billing before API access, even to use the free quota. That's not the "eventual charge after a trial" pattern you explicitly ruled out, but it's close enough in spirit (a payment method on file) that I'm not recommending it without you deciding that's acceptable first.
- **OpenRouter, Cloudflare Workers AI** — both reportedly have standing free tiers, but I didn't independently re-verify their signup flow and card requirements in this pass the way I did the four above. Say the word and I'll do that properly before you sign up for either, rather than you finding out something's off after creating an account.

### 22.4 Loopholes and gotchas worth knowing before you rely on any of this

- **Free-tier terms change without much notice** — Gemini's March 2026 shift is proof of that happening to a major provider mid-project. Nothing here should be treated as "verified forever"; worth a quick re-check every so often, not just once.
- **Free tiers are rate-limited for development, not sized for real production traffic** — fine for this project's local-first, single-user scope; would need re-evaluation if that ever changes.
- **Free-tier AI input isn't always private** — some providers (Gemini's free tier explicitly says so) may use free-tier requests to improve their models. Ollama avoids this entirely since nothing leaves your machine — worth keeping as the default for anything involving your actual trip/property planning data, using the cloud options mainly for experimentation.
- **A GeoNames account with web services *not* enabled looks identical to a broken key** — the two-step signup above exists specifically to avoid that dead end.
- **None of this is wired into the app yet** — creating these accounts is prep, not a working integration; [§21](#21-phased-plan) is the order in which they'd actually get used.
