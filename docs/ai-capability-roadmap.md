# AI capability roadmap

AI is optional at runtime. Integration now exposes a place-suggestion endpoint and owns the Ollama-first provider port. Successful suggestions are saved in `integration.ai_suggestion` with provenance and `DRAFT`/`UNVERIFIED` status. Travel and Property do not yet call it, and no chatbot or acceptance endpoint exists. A stopped Ollama process must leave non-AI workflows available. A generated suggestion remains a **draft** until explicit user acceptance.

## Proposed sequence

1. **Structured trip extraction:** parse a natural-language request into a versioned proposal schema for destinations, dates, travelers, constraints and budget. Reject invalid fields; show the proposed edits before applying them. Property/city extraction uses a separate context schema.
2. **Missing-information detection:** compare a proposed plan to deterministic required-field rules, then let AI explain gaps. Missing fields are computed by Java; the model cannot declare a plan complete.
3. **Tool calling:** expose narrowly scoped read-only tools for current plan data, approved provider quotes and reference data. Validate tool arguments, cap calls and record which tool results were used. Write actions require a separate user acceptance step.
4. **Route explanation:** Java generates, sorts and scores route alternatives from typed legs, costs and durations. AI explains the already-computed trade-offs and cites the alternative IDs; it does not calculate the score.
5. **Price-change summaries:** Java computes amount and percentage changes from immutable snapshots with currency and capture times. AI turns those computed values into plain language without inventing prices.
6. **RAG:** start with source-linked relational records and PostgreSQL full-text search. Add a versioned retrieval corpus, chunk/source IDs and evaluation set if reference volume justifies it. Consider pgvector only if measured retrieval quality or latency cannot be met by simpler search. RAG evidence is never a substitute for an authoritative visa or price source.
7. **Evaluation and hallucination controls:** maintain representative extraction, citation and tool-use cases; test schema validity, acceptance boundaries, factual support and refusal to invent missing data. Ground every claim in retrieved or typed inputs, include source IDs, and mark unsupported claims unverified. Re-evaluate when a prompt, model or provider version changes.

## Suggestion record contract

The current `integration.ai_suggestion` table stores `acceptance_status = DRAFT | ACCEPTED | REJECTED`, model ID, provider, prompt version, generation timestamp, referenced data as JSONB, verification status, and acceptance time. The current endpoint only creates `DRAFT`/`UNVERIFIED` records; acceptance is not implemented. A future accepted plan change must be a separate deterministic application operation with validation and an audit link. AI output never becomes authoritative pricing, visa data or a booking.

Money, totals, percentages, sorting, route scoring and acceptance transitions are calculated and validated in deterministic Java code using typed values and explicit rounding rules. LLMs may only propose or explain them. The provider port must time out cleanly and preserve manual workflows.
