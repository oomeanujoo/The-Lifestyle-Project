package com.thelifestyle.integration.adapter.in.web;

import java.util.List;
import java.util.UUID;

public record PlaceSuggestionResponse(List<String> suggestions, String disclaimer,
                                      UUID draftId, String acceptanceStatus) {}
