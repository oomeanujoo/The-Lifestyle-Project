package com.thelifestyle.travel.domain;

public record DocumentRequirementOutcome(DocumentRequirement documentRequirement, String error) {
    public static DocumentRequirementOutcome ok(DocumentRequirement documentRequirement) {
        return new DocumentRequirementOutcome(documentRequirement, null);
    }

    public static DocumentRequirementOutcome error(String error) {
        return new DocumentRequirementOutcome(null, error);
    }
}
