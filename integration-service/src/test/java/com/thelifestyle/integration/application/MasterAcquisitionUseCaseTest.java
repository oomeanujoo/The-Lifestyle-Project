package com.thelifestyle.integration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelifestyle.integration.adapter.out.ai.AiProviderRouter;
import com.thelifestyle.integration.adapter.out.ai.PromptRegistry;
import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.application.port.out.MasterProposalRepository;
import com.thelifestyle.integration.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.integration.domain.CodedMaster;
import com.thelifestyle.integration.domain.MasterProposal;
import com.thelifestyle.integration.domain.PromptDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MasterAcquisitionUseCaseTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static PromptDefinition bhkTypeDefinition() {
        return new PromptDefinition("bhk_type", "v1", "purpose", "scope",
            List.of("code", "label", "evidence"), java.util.Map.of(),
            List.of("STUDIO", "1RK", "1BHK", "2BHK", "3BHK", "4BHK", "5BHK_PLUS"), null,
            "evidence requirement", "[{\"code\":\"2BHK\",\"label\":\"...\",\"evidence\":\"...\"}]");
    }

    @Test void proposesAVerifiedDraftForAValidCandidate() {
        var promptRegistry = mock(PromptRegistry.class);
        var aiProviderRouter = mock(AiProviderRouter.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var masterProposalRepository = mock(MasterProposalRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);

        when(promptRegistry.find("bhk_type")).thenReturn(Optional.of(bhkTypeDefinition()));
        when(aiProviderRouter.anyProviderConfigured()).thenReturn(true);
        when(aiProviderRouter.complete(any())).thenReturn(new AiProviderRouter.RoutedCompletion(
            "[{\"code\":\"2bhk\",\"label\":\"2 Bedroom Hall Kitchen\",\"evidence\":\"Standard shorthand\"}]", "groq", "model-x"));
        when(codedMasterRepository.findAll(CodedMasterType.BHK_TYPE)).thenReturn(List.of());
        when(masterProposalRepository.findByMasterType("bhk_type")).thenReturn(List.of());

        var useCase = new MasterAcquisitionUseCase(promptRegistry, aiProviderRouter, codedMasterRepository,
            masterProposalRepository, masterRefreshLogRepository, OBJECT_MAPPER);
        var outcome = useCase.acquire(CodedMasterType.BHK_TYPE);

        assertEquals("SUCCESS", outcome.status());
        assertEquals(1, outcome.recordsUpserted());
        verify(masterProposalRepository).create(eq("bhk_type"), anyString(), anyString(), eq("VERIFIED"));
    }

    @Test void failsHonestlyWhenNoProviderIsConfiguredWithoutCreatingAnyProposal() {
        var promptRegistry = mock(PromptRegistry.class);
        var aiProviderRouter = mock(AiProviderRouter.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var masterProposalRepository = mock(MasterProposalRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);

        when(promptRegistry.find("bhk_type")).thenReturn(Optional.of(bhkTypeDefinition()));
        when(aiProviderRouter.anyProviderConfigured()).thenReturn(false);

        var useCase = new MasterAcquisitionUseCase(promptRegistry, aiProviderRouter, codedMasterRepository,
            masterProposalRepository, masterRefreshLogRepository, OBJECT_MAPPER);
        var outcome = useCase.acquire(CodedMasterType.BHK_TYPE);

        assertEquals("FAILED", outcome.status());
        verify(masterProposalRepository, never()).create(any(), any(), any(), any());
    }

    @Test void treatsUnparsableAiOutputAsAnHonestFailureNeverASilentEmptySuccess() {
        var promptRegistry = mock(PromptRegistry.class);
        var aiProviderRouter = mock(AiProviderRouter.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var masterProposalRepository = mock(MasterProposalRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);

        when(promptRegistry.find("bhk_type")).thenReturn(Optional.of(bhkTypeDefinition()));
        when(aiProviderRouter.anyProviderConfigured()).thenReturn(true);
        when(aiProviderRouter.complete(any())).thenReturn(new AiProviderRouter.RoutedCompletion(
            "Sure! Here are some BHK types you might like.", "groq", "model-x"));

        var useCase = new MasterAcquisitionUseCase(promptRegistry, aiProviderRouter, codedMasterRepository,
            masterProposalRepository, masterRefreshLogRepository, OBJECT_MAPPER);
        var outcome = useCase.acquire(CodedMasterType.BHK_TYPE);

        assertEquals("FAILED", outcome.status());
        assertTrue(outcome.errorMessage().contains("not valid JSON"));
        verify(masterProposalRepository, never()).create(any(), any(), any(), any());
    }

    @Test void rejectsACandidateWhoseCodeIsNotInTheAllowedValuesListInsteadOfInventingIt() {
        var promptRegistry = mock(PromptRegistry.class);
        var aiProviderRouter = mock(AiProviderRouter.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var masterProposalRepository = mock(MasterProposalRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);

        when(promptRegistry.find("bhk_type")).thenReturn(Optional.of(bhkTypeDefinition()));
        when(aiProviderRouter.anyProviderConfigured()).thenReturn(true);
        when(aiProviderRouter.complete(any())).thenReturn(new AiProviderRouter.RoutedCompletion(
            "[{\"code\":\"10BHK_MANSION\",\"label\":\"Huge mansion\",\"evidence\":\"made up\"}]", "groq", "model-x"));
        when(codedMasterRepository.findAll(CodedMasterType.BHK_TYPE)).thenReturn(List.of());
        when(masterProposalRepository.findByMasterType("bhk_type")).thenReturn(List.of());

        var useCase = new MasterAcquisitionUseCase(promptRegistry, aiProviderRouter, codedMasterRepository,
            masterProposalRepository, masterRefreshLogRepository, OBJECT_MAPPER);
        var outcome = useCase.acquire(CodedMasterType.BHK_TYPE);

        assertEquals("PARTIAL", outcome.status());
        assertEquals(0, outcome.recordsUpserted());
        assertTrue(outcome.errorMessage().contains("invalid"));
        verify(masterProposalRepository, never()).create(any(), any(), any(), any());
    }

    @Test void rejectsACandidateThatAlreadyExistsAsARealMasterRow() {
        var promptRegistry = mock(PromptRegistry.class);
        var aiProviderRouter = mock(AiProviderRouter.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var masterProposalRepository = mock(MasterProposalRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);

        when(promptRegistry.find("bhk_type")).thenReturn(Optional.of(bhkTypeDefinition()));
        when(aiProviderRouter.anyProviderConfigured()).thenReturn(true);
        when(aiProviderRouter.complete(any())).thenReturn(new AiProviderRouter.RoutedCompletion(
            "[{\"code\":\"2BHK\",\"label\":\"2 Bedroom Hall Kitchen\",\"evidence\":\"...\"}]", "groq", "model-x"));
        when(codedMasterRepository.findAll(CodedMasterType.BHK_TYPE))
            .thenReturn(List.of(new CodedMaster(UUID.randomUUID(), "2BHK", "2 Bedroom Hall Kitchen")));
        when(masterProposalRepository.findByMasterType("bhk_type")).thenReturn(List.of());

        var useCase = new MasterAcquisitionUseCase(promptRegistry, aiProviderRouter, codedMasterRepository,
            masterProposalRepository, masterRefreshLogRepository, OBJECT_MAPPER);
        var outcome = useCase.acquire(CodedMasterType.BHK_TYPE);

        assertEquals("PARTIAL", outcome.status());
        assertTrue(outcome.errorMessage().contains("duplicate"));
        verify(masterProposalRepository, never()).create(any(), any(), any(), any());
    }

    @Test void rejectsACandidateThatDuplicatesAnAlreadyPendingDraftProposal() {
        var promptRegistry = mock(PromptRegistry.class);
        var aiProviderRouter = mock(AiProviderRouter.class);
        var codedMasterRepository = mock(CodedMasterRepository.class);
        var masterProposalRepository = mock(MasterProposalRepository.class);
        var masterRefreshLogRepository = mock(MasterRefreshLogRepository.class);

        when(promptRegistry.find("bhk_type")).thenReturn(Optional.of(bhkTypeDefinition()));
        when(aiProviderRouter.anyProviderConfigured()).thenReturn(true);
        when(aiProviderRouter.complete(any())).thenReturn(new AiProviderRouter.RoutedCompletion(
            "[{\"code\":\"3BHK\",\"label\":\"3 Bedroom Hall Kitchen\",\"evidence\":\"...\"}]", "groq", "model-x"));
        when(codedMasterRepository.findAll(CodedMasterType.BHK_TYPE)).thenReturn(List.of());
        when(masterProposalRepository.findByMasterType("bhk_type")).thenReturn(List.of(
            new MasterProposal(UUID.randomUUID(), "bhk_type", "{\"code\":\"3BHK\",\"label\":\"3BHK\"}",
                "groq/model-x", "VERIFIED", "DRAFT", "2026-09-19T00:00:00Z", null)));

        var useCase = new MasterAcquisitionUseCase(promptRegistry, aiProviderRouter, codedMasterRepository,
            masterProposalRepository, masterRefreshLogRepository, OBJECT_MAPPER);
        var outcome = useCase.acquire(CodedMasterType.BHK_TYPE);

        assertEquals("PARTIAL", outcome.status());
        assertTrue(outcome.errorMessage().contains("duplicate"));
        verify(masterProposalRepository, never()).create(any(), any(), any(), any());
    }
}
