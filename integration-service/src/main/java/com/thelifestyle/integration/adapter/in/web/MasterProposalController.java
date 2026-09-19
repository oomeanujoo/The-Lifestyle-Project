package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.application.MasterProposalUseCase;
import com.thelifestyle.integration.domain.MasterProposal;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// The reviewable-draft path for masters with no verified provider
// (visa_requirement, municipality/area/locality/pincode — see
// MasterProposalUseCase's own comment). Deliberately separate from
// MasterDataController's masters — a proposal here is never itself an
// authoritative lifestyle_master row, only ever a DRAFT idea a human
// reviews through /accept or /reject.
@Tag(name = "Master Proposals", description = "Reviewable AI-produced drafts for masters with no verified provider — never authoritative until a human explicitly accepts one.")
@RestController
@RequestMapping("/api/integration/v1/masters/lifestyle/proposals")
public class MasterProposalController {
    private final MasterProposalUseCase useCase;

    public MasterProposalController(MasterProposalUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<?> propose(@RequestBody MasterProposalRequest request) {
        var proposedDataJson = request.proposedData() == null ? null : request.proposedData().toString();
        var outcome = useCase.propose(request.masterType(), proposedDataJson, request.provenance());
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.proposal()));
    }

    // Defaults to DRAFT — the review queue a human actually needs to see;
    // pass status=ACCEPTED or status=REJECTED to see past decisions.
    @GetMapping
    public List<MasterProposalResponse> list(@RequestParam(name = "status", required = false) String status) {
        return useCase.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable UUID id) {
        var outcome = useCase.accept(id);
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.proposal()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id) {
        var outcome = useCase.reject(id);
        if (outcome.error() != null) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(outcome.error()));
        }
        return ResponseEntity.ok(toResponse(outcome.proposal()));
    }

    private MasterProposalResponse toResponse(MasterProposal proposal) {
        return new MasterProposalResponse(proposal.id().toString(), proposal.masterType(), proposal.proposedData(),
            proposal.provenance(), proposal.verificationStatus(), proposal.acceptanceStatus(), proposal.createdAt(), proposal.acceptedAt());
    }
}
