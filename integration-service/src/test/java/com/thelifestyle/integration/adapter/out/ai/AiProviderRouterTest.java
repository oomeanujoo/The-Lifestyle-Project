package com.thelifestyle.integration.adapter.out.ai;

import com.thelifestyle.integration.adapter.out.resilience.HealthStatusRegistry;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pins the 2026-09-19 reorder: Groq → Mistral → Ollama (Ollama moved to
// last resort after live-testing put it well behind both cloud providers
// on this hardware) — and confirms suggestPlacesVerified() still calls
// the right two providers by name (not by list position) after that
// reorder, since indexing into the reordered list would silently call
// the wrong provider under the wrong name.
class AiProviderRouterTest {

    private static ExternalApisProperties properties() {
        var provider = new ExternalApisProperties.Ai.Provider("", "", "", 25, 60000);
        var local = new ExternalApisProperties.Ai.Local("", "", 720, 60, 60000);
        return new ExternalApisProperties(
            new ExternalApisProperties.GeoNames("", "http://api.geonames.org", 10, 60000),
            new ExternalApisProperties.DataGovIn("", "5c2f62fe-5afa-4119-a499-fec9d604d5bd", "https://api.data.gov.in", 10, 60000),
            new ExternalApisProperties.Frankfurter("https://api.frankfurter.dev/v1", 20, 60000),
            new ExternalApisProperties.Ai(local, provider, provider));
    }

    @Test void triesGroqBeforeMistralBeforeOllamaOnlyFallingThroughOnAnEmptyResult() {
        var ollama = mock(OllamaAiProvider.class);
        var groq = mock(GroqAiProvider.class);
        var mistral = mock(MistralAiProvider.class);
        when(ollama.name()).thenReturn("ollama");
        when(groq.name()).thenReturn("groq");
        when(mistral.name()).thenReturn("mistral");
        when(ollama.model()).thenReturn("qwen3:4b-instruct");
        when(ollama.isConfigured()).thenReturn(true);
        when(groq.isConfigured()).thenReturn(true);
        when(mistral.isConfigured()).thenReturn(true);
        when(groq.suggestPlaces(anyString(), anyInt())).thenReturn(List.of());
        when(mistral.suggestPlaces(anyString(), anyInt())).thenReturn(List.of());
        when(ollama.suggestPlaces(anyString(), anyInt())).thenReturn(List.of("Pune"));

        var router = new AiProviderRouter(ollama, groq, mistral, properties(), new ResilienceGuard(new HealthStatusRegistry()));
        var result = router.suggestPlaces("Pu", 5);

        assertEquals("ollama", result.provider());
        assertEquals(List.of("Pune"), result.suggestions());

        var order = inOrder(groq, mistral, ollama);
        order.verify(groq).suggestPlaces(anyString(), anyInt());
        order.verify(mistral).suggestPlaces(anyString(), anyInt());
        order.verify(ollama).suggestPlaces(anyString(), anyInt());
    }

    @Test void verifiedSuggestionsStillCallOllamaAndGroqByNameNeverMistral() {
        var ollama = mock(OllamaAiProvider.class);
        var groq = mock(GroqAiProvider.class);
        var mistral = mock(MistralAiProvider.class);
        when(ollama.name()).thenReturn("ollama");
        when(groq.name()).thenReturn("groq");
        when(mistral.name()).thenReturn("mistral");
        when(ollama.isConfigured()).thenReturn(true);
        when(groq.isConfigured()).thenReturn(true);
        when(ollama.suggestPlaces(anyString(), anyInt())).thenReturn(List.of("Pune"));
        when(groq.suggestPlaces(anyString(), anyInt())).thenReturn(List.of("Pune"));

        var router = new AiProviderRouter(ollama, groq, mistral, properties(), new ResilienceGuard(new HealthStatusRegistry()));
        var result = router.suggestPlacesVerified("Pu", 5);

        assertEquals(List.of("Pune"), result.ollamaSuggestions());
        assertEquals(List.of("Pune"), result.groqSuggestions());
        verify(mistral, never()).suggestPlaces(anyString(), anyInt());
    }
}
