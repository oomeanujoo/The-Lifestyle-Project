package com.thelifestyle.property.application.port.out;

import com.thelifestyle.property.domain.Place;

import java.util.List;

public interface PlaceRepository {
    List<Place> search(String query);
}
