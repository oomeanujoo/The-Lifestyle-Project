# AI strategy

AI is a core capability but optional at runtime. Future application ports will accept a task and structured inputs and return a validated schema plus provider name, generation time, source references, confidence or verification status and user approval status. Ollama with a local open-weight model is the zero-cost default; a local OpenAI-compatible inference server is an alternate adapter. A paid provider requires explicit user choice. No model download or chatbot is included in Phase 1.

Planned uses: natural language to structured trip plan; natural language to city/property comparison; transport leg suggestions; itinerary improvement; budget explanation; price-change summary; missing-information detection; and PDF narrative generation. Generated prices and visa information are never authoritative. Each suggestion is reviewable before applying it. Provider failure leaves manual planning usable. The Spring AI dependency is deferred until a concrete use case and adapter are implemented.

Every suggestion is a draft until accepted. Its future record includes model/provider, prompt version, generation timestamp, referenced data, verification and user acceptance status. Java computes prices, totals, percentages, sorting and route scores. See [AI capability roadmap](ai-capability-roadmap.md) for the implementation and evaluation sequence.
