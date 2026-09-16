package com.thelifestyle.integration.domain;

import java.util.List;
import java.util.UUID;

// disclaimer is always non-null when this comes from PlaceSuggestionUseCase
// — an AI suggestion is never returned without one attached (§10, AI never
// authoritative). suggestions is empty when no provider is configured or
// every configured provider failed/was rate-limited.
public record PlaceSuggestionResult(List<String> suggestions, String disclaimer,
                                    UUID draftId, String acceptanceStatus) {}
