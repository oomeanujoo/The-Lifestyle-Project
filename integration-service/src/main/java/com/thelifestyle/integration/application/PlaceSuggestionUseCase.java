package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.ai.AiProviderRouter;
import com.thelifestyle.integration.application.port.out.AiSuggestionRepository;
import com.thelifestyle.integration.domain.PlaceSuggestionResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

// The use case travel-service/property-service call (over HTTP, once
// wired) when their own DB-first search finds nothing locally — see
// TECHNICAL_ARCHITECTURE.md §18. This is the one place that decides the
// query is even worth asking an AI provider about, and the one place that
// attaches the "unverified" disclaimer every AI suggestion must carry.
@Service
public class PlaceSuggestionUseCase {
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_SUGGESTIONS = 3;
    private static final String PROMPT_VERSION = "place-suggestion-v1";
    private static final String DRAFT_DISCLAIMER =
        "AI-suggested — unverified, not from any saved data. Treat as a starting point, not a fact.";
    private static final String NO_PROVIDER_NOTICE =
        "No AI provider is configured — set OLLAMA_MODEL, GROQ_API_KEY or MISTRAL_API_KEY "
            + "(see TECHNICAL_ARCHITECTURE.md §18/§22).";

    private final AiProviderRouter aiProviderRouter;
    private final AiSuggestionRepository repository;

    public PlaceSuggestionUseCase(AiProviderRouter aiProviderRouter, AiSuggestionRepository repository) {
        this.aiProviderRouter = aiProviderRouter;
        this.repository = repository;
    }

    public PlaceSuggestionResult suggest(String rawQuery) {
        var query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < MIN_QUERY_LENGTH) return new PlaceSuggestionResult(List.of(), null, null, null);
        if (!aiProviderRouter.anyProviderConfigured()) return new PlaceSuggestionResult(List.of(), NO_PROVIDER_NOTICE, null, null);

        var routed = aiProviderRouter.suggestPlaces(query, MAX_SUGGESTIONS);
        if (routed.suggestions().isEmpty()) return new PlaceSuggestionResult(List.of(), DRAFT_DISCLAIMER, null, null);
        var id = UUID.randomUUID();
        repository.saveDraft(id, query, routed.suggestions(), routed.provider(), routed.model(),
            PROMPT_VERSION, Instant.now());
        return new PlaceSuggestionResult(routed.suggestions(), DRAFT_DISCLAIMER, id, "DRAFT");
    }
}
