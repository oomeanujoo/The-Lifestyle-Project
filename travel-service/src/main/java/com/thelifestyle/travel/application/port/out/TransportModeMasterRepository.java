package com.thelifestyle.travel.application.port.out;

// Upsert-and-append only (§17.1) — keyed by code, already UNIQUE since V2.
public interface TransportModeMasterRepository {
    void upsert(String code, String label);
    long count();
}
