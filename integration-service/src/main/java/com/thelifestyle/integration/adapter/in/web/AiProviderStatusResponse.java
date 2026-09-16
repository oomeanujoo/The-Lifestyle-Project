package com.thelifestyle.integration.adapter.in.web;

// health is "UP" | "DOWN" | "UNKNOWN" — the outcome of the last real call
// this process made to this provider, not a fresh ping (§18). "UNKNOWN"
// means either never called yet this process's lifetime, or not configured
// at all — the frontend Dashboard widget (§20) maps: not configured →
// orange, health UP → green, health DOWN → red, health UNKNOWN → orange.
public record AiProviderStatusResponse(String name, boolean configured, String health, String lastCheckedAt, String lastError) {}
