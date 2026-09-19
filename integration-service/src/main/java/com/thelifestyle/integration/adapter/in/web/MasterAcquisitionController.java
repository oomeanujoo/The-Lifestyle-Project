package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.application.MasterAcquisitionUseCase;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Prompt-driven acquisition (§21's "backend AI master engine" task) for the
// three flat, FK-free coded masters — transport_mode/bhk_type/service_addon.
// Fully exercised for bhk_type per that task's own instruction; the other
// two already work through the same code path, since nothing here
// special-cases bhk_type. city/currency are deliberately NOT reachable
// through this controller — they keep their real, verified providers
// (GeoNames/Frankfurter) and must be triggered via MasterDataController's
// /refresh instead. Every call here only ever produces DRAFT
// master_proposal rows (see MasterAcquisitionUseCase's own comment) —
// never a direct write to a real lifestyle_master table.
@Tag(name = "Master Acquisition", description = "Prompt-driven AI drafting for coded masters with no independent verified provider — always produces reviewable DRAFT proposals, never a direct master write.")
@RestController
@RequestMapping("/api/integration/v1/masters/lifestyle")
public class MasterAcquisitionController {
    private final MasterAcquisitionUseCase useCase;

    public MasterAcquisitionController(MasterAcquisitionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/bhk-types/acquire")
    public MasterRefreshOutcomeResponse acquireBhkTypes() {
        return toResponse(useCase.acquire(CodedMasterType.BHK_TYPE));
    }

    @PostMapping("/transport-modes/acquire")
    public MasterRefreshOutcomeResponse acquireTransportModes() {
        return toResponse(useCase.acquire(CodedMasterType.TRANSPORT_MODE));
    }

    @PostMapping("/service-addons/acquire")
    public MasterRefreshOutcomeResponse acquireServiceAddons() {
        return toResponse(useCase.acquire(CodedMasterType.SERVICE_ADDON));
    }

    private MasterRefreshOutcomeResponse toResponse(com.thelifestyle.integration.domain.MasterRefreshOutcome outcome) {
        return new MasterRefreshOutcomeResponse(outcome.masterName(), outcome.status(), outcome.recordsUpserted(), outcome.errorMessage());
    }
}
