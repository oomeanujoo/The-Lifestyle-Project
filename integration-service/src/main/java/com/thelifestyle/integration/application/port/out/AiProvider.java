package com.thelifestyle.integration.application.port.out;

import java.util.List;

// A single AI provider that can draft plausible place-name suggestions, or
// answer an arbitrary structured prompt — never authoritative, never a
// source of truth (see §10, "AI never asserts a fact"). isConfigured() lets
// callers skip a provider with no API key set without treating that as a
// failure.
public interface AiProvider {
    String name();
    String model();
    boolean isConfigured();
    List<String> suggestPlaces(String query, int maxSuggestions);

    // A raw chat completion for callers with their own prompt and their own
    // parsing of the response — e.g. MasterAcquisitionUseCase, which sends a
    // versioned PromptDefinition's prompt and treats the reply as untrusted
    // text to validate/normalize itself, never as a fact. Returns null (not
    // an exception) on any failure or when not configured, matching
    // suggestPlaces()'s "empty means skip" convention.
    String complete(String prompt);
}
