/**
 * Outbound ports the application layer depends on but doesn't implement —
 * e.g. {@link com.thelifestyle.property.application.port.out.PlaceRepository}.
 * Adapters in {@code adapter.out.persistence} implement these; swapping the
 * in-memory placeholder for a real JPA adapter later means adding one new
 * class, not touching the application layer at all.
 */
package com.thelifestyle.property.application.port.out;
