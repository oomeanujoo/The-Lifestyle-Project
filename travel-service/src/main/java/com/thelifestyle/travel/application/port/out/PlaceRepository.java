package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.Place;

import java.util.List;

public interface PlaceRepository {
    List<Place> search(String query);
}
