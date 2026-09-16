package com.thelifestyle.integration.application.port.out;

import java.util.List;

// A single AI provider that can draft plausible place-name suggestions —
// never authoritative, never a source of truth (see §10, "AI never asserts
// a fact"). isConfigured() lets callers skip a provider with no API key set
// without treating that as a failure.
public interface AiProvider {
    String name();
    String model();
    boolean isConfigured();
    List<String> suggestPlaces(String query, int maxSuggestions);
}
