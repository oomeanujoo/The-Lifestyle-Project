package com.thelifestyle.integration.domain;

import java.util.List;
import java.util.Map;

// A versioned, file-backed definition of what to ask an AI provider for one
// master type, and exactly how to judge what comes back — never a prompt
// improvised inline in Java. `version` is part of the file name
// (`{masterType}.v{version}.json`) and is recorded as provenance on every
// proposal it produces, so a later, better-worded prompt never silently
// changes what an already-recorded proposal was judged against.
// `allowedValues`/`codeFormatRegex` are what "verification" means for a
// master with no independent provider (§19): a candidate code checked
// against a fixed, human-authored rule here is deterministically verified;
// the AI having said it, or two AI providers agreeing, is not (per this
// task's own instruction — a model's self-asserted citation or provider
// agreement is never independent verification).
public record PromptDefinition(
    String masterType,
    String version,
    String purpose,
    String scope,
    List<String> requiredFields,
    Map<String, String> fieldFormats,
    List<String> allowedValues,
    String codeFormatRegex,
    String evidenceRequirements,
    String exampleResponse) {

    // Renders this definition into the literal text sent to the AI
    // provider — every constraint the definition carries is spelled out,
    // never left implicit, so validating the response against the same
    // definition afterward is checking the model actually followed
    // instructions, not guessing at what it might have meant.
    public String buildPrompt() {
        var sb = new StringBuilder();
        sb.append("You are drafting candidate reference-data rows for a real application. ")
            .append("Purpose: ").append(purpose).append("\n")
            .append("Scope: ").append(scope).append("\n")
            .append("Required fields per candidate: ").append(String.join(", ", requiredFields)).append("\n");
        fieldFormats.forEach((field, format) -> sb.append("Field '").append(field).append("' format: ").append(format).append("\n"));
        if (allowedValues != null && !allowedValues.isEmpty()) {
            sb.append("The 'code' field MUST be one of exactly these values, verbatim: ")
                .append(String.join(", ", allowedValues)).append("\n");
        }
        if (codeFormatRegex != null && !codeFormatRegex.isBlank()) {
            sb.append("The 'code' field MUST match this pattern: ").append(codeFormatRegex).append("\n");
        }
        sb.append("Evidence requirement: ").append(evidenceRequirements).append("\n")
            .append("Reply with ONLY a JSON array of candidate objects, no prose, no markdown fences, no explanation. ")
            .append("Example of the exact shape expected:\n").append(exampleResponse);
        return sb.toString();
    }
}
