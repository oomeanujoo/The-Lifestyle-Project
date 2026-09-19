package com.thelifestyle.integration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.application.port.out.MasterProposalRepository;
import com.thelifestyle.integration.domain.MasterProposal;
import com.thelifestyle.integration.domain.MasterProposalOutcome;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// The reviewable-draft path for master data with no verified provider.
// Two different kinds of proposal end up in the same table, accepted
// through the same endpoint, but acceptance means something different for
// each:
//  - visa_requirement / the municipality-area-locality-pincode hierarchy:
//    real FK/hierarchy constraints a generic accept step cannot safely
//    satisfy, so accepting one here ONLY flips its own status — turning it
//    into a real lifestyle_master row stays a deliberate, separate, manual
//    step through the existing masters API.
//  - transport_mode / bhk_type / service_addon (via MasterAcquisitionUseCase):
//    flat, FK-free code/label rows with an existing safe upsert path
//    (CodedMasterRepository), so accepting one of these here DOES create
//    the real lifestyle_master row — that's the whole point of "acceptance
//    can be a backend API; it does not require a review UI" for this kind
//    of master.
// Either way, nothing is ever promoted before this explicit accept call.
@Service
public class MasterProposalUseCase {
    // The only master types this service has no verified provider AND no
    // safe-to-auto-apply CRUD path for — every other type (city, currency,
    // and the three CodedMasterType ones) already has a real refresh or
    // CRUD path and must use that (or MasterAcquisitionUseCase's proposal
    // path, which calls the repository directly), not this method.
    private static final Set<String> UNVERIFIED_MASTER_TYPES =
        Set.of("visa_requirement", "municipality", "area", "locality", "pincode");

    private final MasterProposalRepository repository;
    private final CodedMasterRepository codedMasterRepository;
    private final ObjectMapper objectMapper;

    public MasterProposalUseCase(MasterProposalRepository repository, CodedMasterRepository codedMasterRepository,
                                  ObjectMapper objectMapper) {
        this.repository = repository;
        this.codedMasterRepository = codedMasterRepository;
        this.objectMapper = objectMapper;
    }

    public MasterProposalOutcome propose(String masterType, String proposedDataJson, String provenance) {
        if (!StringUtils.hasText(masterType) || !UNVERIFIED_MASTER_TYPES.contains(masterType)) {
            return MasterProposalOutcome.error(
                "masterType '" + masterType + "' must be one of " + UNVERIFIED_MASTER_TYPES
                    + " — every other master has a verified provider or CRUD path and must use that instead");
        }
        if (!StringUtils.hasText(proposedDataJson)) {
            return MasterProposalOutcome.error("proposedData is required");
        }
        if (!StringUtils.hasText(provenance)) {
            return MasterProposalOutcome.error("provenance is required — say which AI provider/model produced this idea");
        }

        return MasterProposalOutcome.ok(repository.create(masterType, proposedDataJson, provenance.trim()));
    }

    public List<MasterProposal> findByStatus(String acceptanceStatus) {
        return StringUtils.hasText(acceptanceStatus) ? repository.findByStatus(acceptanceStatus.toUpperCase()) : repository.findByStatus("DRAFT");
    }

    public MasterProposalOutcome accept(UUID id) {
        var existing = repository.findById(id);
        if (existing.isEmpty()) {
            return MasterProposalOutcome.error("proposal '" + id + "' does not exist");
        }
        if (!"DRAFT".equals(existing.get().acceptanceStatus())) {
            return MasterProposalOutcome.error("proposal '" + id + "' is not currently DRAFT — it may already be accepted or rejected");
        }

        // Apply BEFORE flipping status, not after: if this fails (malformed
        // proposedData, or the code was created some other way in the
        // meantime), the proposal must stay DRAFT so it can be retried or
        // rejected — never ACCEPTED with no matching master row to show for it.
        var codedType = CodedMasterType.fromMasterName(existing.get().masterType());
        if (codedType.isPresent() && applyToCodedMaster(codedType.get(), existing.get().proposedData()).isEmpty()) {
            return MasterProposalOutcome.error("proposal '" + id + "' could not be applied to " + codedType.get().table()
                + " — proposedData is missing/malformed 'code' or 'label', or that code already exists there");
        }

        return repository.accept(id)
            .map(MasterProposalOutcome::ok)
            .orElseGet(() -> MasterProposalOutcome.error("proposal '" + id + "' changed state before it could be accepted — try again"));
    }

    public MasterProposalOutcome reject(UUID id) {
        return repository.reject(id)
            .map(MasterProposalOutcome::ok)
            .orElseGet(() -> MasterProposalOutcome.error(
                "proposal '" + id + "' does not exist or is not currently DRAFT — it may already be accepted or rejected"));
    }

    private Optional<com.thelifestyle.integration.domain.CodedMaster> applyToCodedMaster(CodedMasterType type, String proposedDataJson) {
        try {
            var node = objectMapper.readTree(proposedDataJson);
            var codeNode = node.get("code");
            var labelNode = node.get("label");
            var code = codeNode == null ? null : codeNode.asText();
            var label = labelNode == null ? null : labelNode.asText();
            if (code == null || code.isBlank() || label == null || label.isBlank()) return Optional.empty();
            if (codedMasterRepository.findAll(type).stream().anyMatch(m -> m.code().equalsIgnoreCase(code))) return Optional.empty();
            return Optional.of(codedMasterRepository.upsert(type, code, label));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
