CREATE TABLE integration.ai_suggestion (
    id uuid PRIMARY KEY,
    query_text text NOT NULL,
    suggestions text[] NOT NULL,
    provider varchar(40) NOT NULL,
    model varchar(120) NOT NULL,
    prompt_version varchar(40) NOT NULL,
    generated_at timestamptz NOT NULL,
    referenced_data jsonb NOT NULL DEFAULT '[]'::jsonb,
    verification_status varchar(20) NOT NULL DEFAULT 'UNVERIFIED',
    acceptance_status varchar(20) NOT NULL DEFAULT 'DRAFT',
    accepted_at timestamptz,
    CONSTRAINT ai_suggestion_verification_status_check
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    CONSTRAINT ai_suggestion_acceptance_status_check
        CHECK (acceptance_status IN ('DRAFT', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT ai_suggestion_accepted_at_check
        CHECK (acceptance_status <> 'ACCEPTED' OR accepted_at IS NOT NULL)
);
