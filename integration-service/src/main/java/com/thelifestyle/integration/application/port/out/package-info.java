/**
 * Outbound ports the application layer depends on but doesn't implement —
 * e.g. {@link com.thelifestyle.integration.application.port.out.AiProvider}.
 * Adapters in {@code adapter.out.*} implement these; adding Ollama as a
 * third AI provider, for example, means adding one new class, not touching
 * the application layer at all.
 */
package com.thelifestyle.integration.application.port.out;
