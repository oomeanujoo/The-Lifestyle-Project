package com.thelifestyle.travel.application;

import com.thelifestyle.travel.adapter.out.persistence.InMemoryPlaceRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceSearchUseCaseTest {
    private final PlaceSearchUseCase useCase = new PlaceSearchUseCase(new InMemoryPlaceRepository());

    @Test void returnsNoResultsBelowMinimumQueryLength() {
        var result = useCase.search("P");
        assertTrue(result.matches().isEmpty());
        assertNull(result.aiFallbackNotice());
    }

    @Test void findsKnownPlaceCaseInsensitively() {
        var result = useCase.search("pune");
        assertEquals(1, result.matches().size());
        assertEquals("Pune", result.matches().get(0).label());
        assertNull(result.aiFallbackNotice());
    }

    @Test void returnsAiFallbackNoticeWhenNothingMatchesLocally() {
        var result = useCase.search("Antarctica");
        assertTrue(result.matches().isEmpty());
        assertNotNull(result.aiFallbackNotice());
    }
}
