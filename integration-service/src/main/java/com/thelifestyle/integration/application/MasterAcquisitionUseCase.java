package com.thelifestyle.integration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelifestyle.integration.adapter.out.ai.AiProviderRouter;
import com.thelifestyle.integration.adapter.out.ai.PromptRegistry;
import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.application.port.out.MasterProposalRepository;
import com.thelifestyle.integration.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.integration.domain.MasterRefreshOutcome;
import com.thelifestyle.integration.domain.PromptDefinition;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

// Prompt-driven acquisition for a CodedMasterType (transport_mode/bhk_type/
// service_addon — all three share the exact code/label shape, so this one
// class is the "reusable pattern," not a per-master framework). Fully
// wired for BHK_TYPE per the task's own instruction ("begin with one
// complete master type"); TRANSPORT_MODE/SERVICE_ADDON work through the
// same method today since their prompt definitions already exist and the
// logic doesn't special-case bhk_type anywhere — calling
// acquire(CodedMasterType.TRANSPORT_MODE) already works, it just hasn't
// been asked for yet. city/currency are NOT acquired this way — they keep
// their real, verified providers (GeoNames/Frankfurter); see those two
// prompt definitions' own "documentation only" note for why they exist as
// files without an acquisition path.
//
// Every candidate becomes, at most, a DRAFT master_proposal row — never a
// direct write to the real lifestyle_master table. "Verified" here means
// the candidate's code matched a fixed, human-authored rule
// (PromptDefinition's allowedValues or codeFormatRegex), checked in Java —
// never because the model said so, and never because multiple providers
// agreed (this task's own instruction: neither is independent verification).
@Service
public class MasterAcquisitionUseCase {
    private final PromptRegistry promptRegistry;
    private final AiProviderRouter aiProviderRouter;
    private final CodedMasterRepository codedMasterRepository;
    private final MasterProposalRepository masterProposalRepository;
    private final MasterRefreshLogRepository masterRefreshLogRepository;
    private final ObjectMapper objectMapper;

    public MasterAcquisitionUseCase(PromptRegistry promptRegistry, AiProviderRouter aiProviderRouter,
                                     CodedMasterRepository codedMasterRepository, MasterProposalRepository masterProposalRepository,
                                     MasterRefreshLogRepository masterRefreshLogRepository, ObjectMapper objectMapper) {
        this.promptRegistry = promptRegistry;
        this.aiProviderRouter = aiProviderRouter;
        this.codedMasterRepository = codedMasterRepository;
        this.masterProposalRepository = masterProposalRepository;
        this.masterRefreshLogRepository = masterRefreshLogRepository;
        this.objectMapper = objectMapper;
    }

    public MasterRefreshOutcome acquire(CodedMasterType type) {
        var startedAt = Instant.now();
        var masterName = type.masterName();

        var definition = promptRegistry.find(masterName);
        if (definition.isEmpty()) {
            var outcome = MasterRefreshOutcome.failed(masterName, "no prompt definition registered for '" + masterName + "'");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        if (!aiProviderRouter.anyProviderConfigured()) {
            var outcome = MasterRefreshOutcome.failed(masterName, "no AI provider is configured (OLLAMA_MODEL, GROQ_API_KEY, or MISTRAL_API_KEY)");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        var completion = aiProviderRouter.complete(definition.get().buildPrompt());
        if (completion.content() == null || completion.content().isBlank()) {
            var outcome = MasterRefreshOutcome.failed(masterName, "every configured AI provider failed or returned nothing");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        List<CodedMasterCandidate> candidates;
        try {
            candidates = objectMapper.readValue(stripMarkdownFence(completion.content()), objectMapper.getTypeFactory()
                .constructCollectionType(List.class, CodedMasterCandidate.class));
        } catch (Exception ex) {
            // Untrusted text that didn't even parse as the requested shape —
            // an honest FAILED outcome, never a silently-empty SUCCESS and
            // never a raw exception escaping to the caller.
            var outcome = MasterRefreshOutcome.failed(masterName, "AI response was not valid JSON: " + ex.getMessage());
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        var existingCodes = codedMasterRepository.findAll(type).stream()
            .map(m -> m.code().toUpperCase(Locale.ROOT))
            .toList();
        // Mutable and seeded from what's already pending — a candidate
        // accepted earlier in this same AI response must also block a
        // second, later candidate in the same batch proposing the exact
        // same code, not just a code from a previous run.
        var pendingCodes = new java.util.HashSet<>(masterProposalRepository.findByMasterType(masterName).stream()
            .filter(p -> "DRAFT".equals(p.acceptanceStatus()))
            .map(p -> extractPendingCode(p.proposedData()))
            .filter(java.util.Objects::nonNull)
            .map(code -> code.toUpperCase(Locale.ROOT))
            .toList());

        var proposed = 0;
        var rejectedInvalid = 0;
        var rejectedDuplicate = 0;
        for (var candidate : candidates) {
            if (candidate.code() == null || candidate.code().isBlank() || candidate.label() == null || candidate.label().isBlank()) {
                rejectedInvalid++;
                continue;
            }
            var normalizedCode = candidate.code().trim().toUpperCase(Locale.ROOT);
            if (!matchesRule(normalizedCode, definition.get())) {
                rejectedInvalid++;
                continue;
            }
            if (existingCodes.contains(normalizedCode) || pendingCodes.contains(normalizedCode)) {
                rejectedDuplicate++;
                continue;
            }

            try {
                var proposedData = new HashMap<String, String>();
                proposedData.put("code", normalizedCode);
                proposedData.put("label", candidate.label().trim());
                proposedData.put("evidence", candidate.evidence());
                proposedData.put("promptVersion", definition.get().version());
                var provenance = completion.provider() + "/" + completion.model() + " via prompts/" + masterName + "." + definition.get().version() + ".json";
                masterProposalRepository.create(masterName, objectMapper.writeValueAsString(proposedData), provenance, "VERIFIED");
                proposed++;
                pendingCodes.add(normalizedCode);
            } catch (Exception ex) {
                rejectedInvalid++;
            }
        }

        var rejected = rejectedInvalid + rejectedDuplicate;
        MasterRefreshOutcome outcome;
        if (proposed == 0 && candidates.isEmpty()) {
            outcome = MasterRefreshOutcome.partial(masterName, 0, "AI returned no candidates");
        } else if (proposed == 0) {
            outcome = MasterRefreshOutcome.partial(masterName, 0,
                "all " + candidates.size() + " candidate(s) rejected (" + rejectedInvalid + " invalid, " + rejectedDuplicate + " duplicate)");
        } else if (rejected > 0) {
            outcome = MasterRefreshOutcome.partial(masterName, proposed,
                proposed + " proposal(s) drafted, " + rejected + " candidate(s) rejected (" + rejectedInvalid + " invalid, " + rejectedDuplicate + " duplicate)");
        } else {
            outcome = MasterRefreshOutcome.success(masterName, proposed);
        }
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }

    private boolean matchesRule(String normalizedCode, PromptDefinition definition) {
        if (definition.allowedValues() != null && !definition.allowedValues().isEmpty()) {
            return definition.allowedValues().contains(normalizedCode);
        }
        if (definition.codeFormatRegex() != null && !definition.codeFormatRegex().isBlank()) {
            return normalizedCode.matches(definition.codeFormatRegex());
        }
        // No deterministic rule defined for this master type at all — never
        // silently accept an unrule-checked code as "verified" by omission.
        return false;
    }

    private String extractPendingCode(String proposedDataJson) {
        try {
            var node = objectMapper.readTree(proposedDataJson);
            var codeNode = node.get("code");
            return codeNode == null ? null : codeNode.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String stripMarkdownFence(String content) {
        var trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").trim();
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }
}
