package com.thelifestyle.property.application.port.out;

// Upsert-and-append only (§17.1) — keyed by code, already UNIQUE since V2.
public interface BhkTypeMasterRepository {
    void upsert(String code, String label);
    long count();
}
