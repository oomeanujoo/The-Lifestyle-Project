package com.thelifestyle.integration.domain;

public record MasterProposalOutcome(MasterProposal proposal, String error) {
    public static MasterProposalOutcome ok(MasterProposal proposal) {
        return new MasterProposalOutcome(proposal, null);
    }

    public static MasterProposalOutcome error(String error) {
        return new MasterProposalOutcome(null, error);
    }
}
