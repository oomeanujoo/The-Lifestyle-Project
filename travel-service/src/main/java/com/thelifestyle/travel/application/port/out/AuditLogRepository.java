package com.thelifestyle.travel.application.port.out;

import java.util.UUID;

// One generic audit_log table for every transaction table (§16), not one
// audit table per transaction table — per the generic-over-bespoke rule
// (§10). Written automatically alongside every transaction-table write;
// `diffJson` is left to the table's own DEFAULT '{}'::jsonb for now — a
// real "what specifically changed" payload is a deliberate future
// enhancement, not required for entity_type/entity_id/action/changed_at
// to already be a real, honest audit trail. `changedBy` is always null —
// this app has no accounts/auth yet (§20), so there is no user identity
// to honestly record.
public interface AuditLogRepository {
    void record(String entityType, UUID entityId, String action);
}
