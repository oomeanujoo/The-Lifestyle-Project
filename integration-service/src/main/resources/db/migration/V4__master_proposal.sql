-- A single, generic table for any AI-produced master-data idea that has no
-- verified provider behind it (visa_requirement text, or the
-- municipality/area/locality/pincode hierarchy) — never one table per
-- master type, matching the audit_log/master_refresh_log convention of one
-- reused table over several bespoke ones. Every row starts and stays
-- acceptance_status='DRAFT' until a human explicitly accepts or rejects
-- it here; nothing in this codebase writes AI-produced text into an
-- authoritative lifestyle_master table automatically. Same
-- verification/acceptance status pair and CHECK constraints as
-- integration.ai_suggestion (V1), for the same reason.
CREATE TABLE integration.master_proposal (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    master_type varchar(40) NOT NULL,
    proposed_data jsonb NOT NULL,
    provenance varchar(160) NOT NULL,
    verification_status varchar(20) NOT NULL DEFAULT 'UNVERIFIED',
    acceptance_status varchar(20) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL DEFAULT now(),
    accepted_at timestamptz,
    CONSTRAINT integration_master_proposal_verification_check
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    CONSTRAINT integration_master_proposal_acceptance_check
        CHECK (acceptance_status IN ('DRAFT', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT integration_master_proposal_accepted_at_check
        CHECK (acceptance_status <> 'ACCEPTED' OR accepted_at IS NOT NULL)
);
CREATE INDEX integration_master_proposal_type_status_idx
    ON integration.master_proposal (master_type, acceptance_status, created_at DESC);
