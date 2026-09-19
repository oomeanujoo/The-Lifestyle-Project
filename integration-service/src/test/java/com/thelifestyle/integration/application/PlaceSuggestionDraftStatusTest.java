package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.ai.AiProviderRouter;
import com.thelifestyle.integration.application.port.out.AiSuggestionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pins the explicit instruction "keep AI output as an unaccepted DRAFT" —
// a successful AI suggestion must always come back and be persisted with
// acceptance_status DRAFT, never anything else, regardless of which
// provider answered. AiProviderRouter is mocked (Mockito, already bundled
// via spring-boot-starter-test) rather than exercised for real, since a
// real call would need actual provider credentials and hit the network.
class PlaceSuggestionDraftStatusTest {

    @Test void successfulSuggestionIsAlwaysSavedAndReturnedAsDraft() {
        var router = mock(AiProviderRouter.class);
        when(router.anyProviderConfigured()).thenReturn(true);
        when(router.suggestPlaces(any(), anyInt()))
            .thenReturn(new AiProviderRouter.RoutedSuggestion(List.of("Dubai Marina", "Downtown Dubai"), "groq", "test-model"));

        var repository = mock(AiSuggestionRepository.class);
        var useCase = new PlaceSuggestionUseCase(router, repository);

        var result = useCase.suggest("Dubai");

        assertEquals(List.of("Dubai Marina", "Downtown Dubai"), result.suggestions());
        assertEquals("DRAFT", result.acceptanceStatus());

        var providerCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository, times(1)).saveDraft(
            any(UUID.class), any(String.class), any(List.class),
            providerCaptor.capture(), any(String.class), any(String.class), any(Instant.class));
        assertEquals("groq", providerCaptor.getValue());
    }
}
