package com.thelifestyle.property.application;

import com.thelifestyle.property.adapter.out.persistence.InMemoryPlaceRepository;
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

    @Test void disambiguatesLocalitiesWithParentContext() {
        var result = useCase.search("Gandhi Nagar");
        assertEquals(1, result.matches().size());
        assertEquals("Gandhi Nagar, Aundh", result.matches().get(0).label());
    }

    @Test void returnsAiFallbackNoticeWhenNothingMatchesLocally() {
        var result = useCase.search("Antarctica");
        assertTrue(result.matches().isEmpty());
        assertNotNull(result.aiFallbackNotice());
    }
}
