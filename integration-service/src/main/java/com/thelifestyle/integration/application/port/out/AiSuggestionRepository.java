package com.thelifestyle.integration.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiSuggestionRepository {
    void saveDraft(UUID id, String query, List<String> suggestions, String provider,
                   String model, String promptVersion, Instant generatedAt);
}
