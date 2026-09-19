package com.thelifestyle.integration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.application.port.out.MasterProposalRepository;
import com.thelifestyle.integration.domain.CodedMaster;
import com.thelifestyle.integration.domain.MasterProposal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MasterProposalUseCaseTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test void createsADraftProposalForAnUnverifiedMasterType() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var created = new MasterProposal(UUID.randomUUID(), "visa_requirement", "{\"requirementText\":\"...\"}",
            "groq/openai-oss-20b", "UNVERIFIED", "DRAFT", "2026-09-19T00:00:00Z", null);
        when(repository.create("visa_requirement", "{\"requirementText\":\"...\"}", "groq/openai-oss-20b")).thenReturn(created);

        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);
        var outcome = useCase.propose("visa_requirement", "{\"requirementText\":\"...\"}", "groq/openai-oss-20b");

        assertNull(outcome.error());
        assertEquals(created, outcome.proposal());
        assertEquals("DRAFT", outcome.proposal().acceptanceStatus());
    }

    @Test void refusesAMasterTypeThatAlreadyHasAVerifiedProviderOrCrudPath() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);

        var outcome = useCase.propose("city", "{}", "groq/openai-oss-20b");

        assertTrue(outcome.error().contains("city"));
        verify(repository, never()).create(any(), any(), any());
    }

    @Test void refusesABlankProvenance() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);

        var outcome = useCase.propose("locality", "{}", "  ");

        assertTrue(outcome.error().contains("provenance"));
        verify(repository, never()).create(any(), any(), any());
    }

    @Test void acceptingAProposalThatIsNotCurrentlyDraftIsAnHonestErrorNotASilentNoOp() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var id = UUID.randomUUID();
        var alreadyAccepted = new MasterProposal(id, "municipality", "{}", "manual review",
            "UNVERIFIED", "ACCEPTED", "2026-09-19T00:00:00Z", "2026-09-19T01:00:00Z");
        when(repository.findById(id)).thenReturn(Optional.of(alreadyAccepted));

        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);
        var outcome = useCase.accept(id);

        assertTrue(outcome.error().contains(id.toString()));
    }

    @Test void acceptingAProposalThatDoesNotExistIsAnHonestError() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);
        var outcome = useCase.accept(id);

        assertTrue(outcome.error().contains(id.toString()));
    }

    @Test void acceptsADraftProposalForAHierarchicalMasterTypeWithoutTouchingAnyRealMasterTable() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var id = UUID.randomUUID();
        var draft = new MasterProposal(id, "municipality", "{}", "manual review",
            "UNVERIFIED", "DRAFT", "2026-09-19T00:00:00Z", null);
        var accepted = new MasterProposal(id, "municipality", "{}", "manual review",
            "UNVERIFIED", "ACCEPTED", "2026-09-19T00:00:00Z", "2026-09-19T01:00:00Z");
        when(repository.findById(id)).thenReturn(Optional.of(draft));
        when(repository.accept(id)).thenReturn(Optional.of(accepted));

        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);
        var outcome = useCase.accept(id);

        assertNull(outcome.error());
        assertEquals("ACCEPTED", outcome.proposal().acceptanceStatus());
        verify(codedMasterRepository, never()).upsert(any(), any(), any());
    }

    @Test void acceptingACodedMasterProposalWritesTheRealMasterRowBeforeFlippingStatus() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var id = UUID.randomUUID();
        var draft = new MasterProposal(id, "bhk_type", "{\"code\":\"2BHK\",\"label\":\"2 Bedroom Hall Kitchen\"}",
            "groq/openai-oss-20b via prompts/bhk_type.v1.json", "VERIFIED", "DRAFT", "2026-09-19T00:00:00Z", null);
        var accepted = new MasterProposal(id, "bhk_type", draft.proposedData(), draft.provenance(),
            "VERIFIED", "ACCEPTED", "2026-09-19T00:00:00Z", "2026-09-19T01:00:00Z");
        when(repository.findById(id)).thenReturn(Optional.of(draft));
        when(codedMasterRepository.findAll(CodedMasterType.BHK_TYPE)).thenReturn(List.of());
        when(codedMasterRepository.upsert(CodedMasterType.BHK_TYPE, "2BHK", "2 Bedroom Hall Kitchen"))
            .thenReturn(new CodedMaster(UUID.randomUUID(), "2BHK", "2 Bedroom Hall Kitchen"));
        when(repository.accept(id)).thenReturn(Optional.of(accepted));

        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);
        var outcome = useCase.accept(id);

        assertNull(outcome.error());
        assertEquals("ACCEPTED", outcome.proposal().acceptanceStatus());
        verify(codedMasterRepository).upsert(CodedMasterType.BHK_TYPE, "2BHK", "2 Bedroom Hall Kitchen");
    }

    @Test void refusesToAcceptACodedMasterProposalWhoseCodeAlreadyExistsAndLeavesItDraft() {
        var repository = mock(MasterProposalRepository.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var id = UUID.randomUUID();
        var draft = new MasterProposal(id, "bhk_type", "{\"code\":\"2BHK\",\"label\":\"2 Bedroom Hall Kitchen\"}",
            "groq/openai-oss-20b", "VERIFIED", "DRAFT", "2026-09-19T00:00:00Z", null);
        when(repository.findById(id)).thenReturn(Optional.of(draft));
        when(codedMasterRepository.findAll(CodedMasterType.BHK_TYPE))
            .thenReturn(List.of(new CodedMaster(UUID.randomUUID(), "2BHK", "2 Bedroom Hall Kitchen")));

        var useCase = new MasterProposalUseCase(repository, codedMasterRepository, OBJECT_MAPPER);
        var outcome = useCase.accept(id);

        assertTrue(outcome.error().contains(id.toString()));
        verify(repository, never()).accept(any());
        verify(codedMasterRepository, never()).upsert(eq(CodedMasterType.BHK_TYPE), any(), any());
    }
}
