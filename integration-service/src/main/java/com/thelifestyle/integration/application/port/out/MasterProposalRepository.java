package com.thelifestyle.integration.application.port.out;

import com.thelifestyle.integration.domain.MasterProposal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// integration.master_proposal — one generic table for any AI-produced idea
// for a master with no verified provider, reused across master types
// rather than one table per type (same convention as ai_suggestion/
// master_refresh_log). No update method for `proposedData` itself: a
// proposal is either accepted, rejected, or superseded by a fresh proposal
// — never silently edited in place.
public interface MasterProposalRepository {
    MasterProposal create(String masterType, String proposedDataJson, String provenance);

    // For a caller that has already deterministically checked the
    // candidate against a fixed rule (e.g. MasterAcquisitionUseCase
    // matching a code against a PromptDefinition's allowedValues/
    // codeFormatRegex) — never for "the AI said so" or "two providers
    // agreed," neither of which this codebase treats as verification.
    MasterProposal create(String masterType, String proposedDataJson, String provenance, String verificationStatus);

    List<MasterProposal> findByStatus(String acceptanceStatus);
    List<MasterProposal> findByMasterType(String masterType);
    Optional<MasterProposal> findById(UUID id);
    Optional<MasterProposal> accept(UUID id);
    Optional<MasterProposal> reject(UUID id);
}
