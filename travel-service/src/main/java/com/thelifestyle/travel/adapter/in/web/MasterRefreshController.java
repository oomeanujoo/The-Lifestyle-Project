package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.MasterRefreshUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Backs the Settings page's "Refresh master data" action (§20). POST
// triggers a real, synchronous refresh (city + currency, via
// integration-service) and returns exactly what happened — GET returns
// live counts/timestamps read straight from Postgres, nothing cached or
// invented in this class.
@RestController
@RequestMapping("/api/travel/v1/masters")
public class MasterRefreshController {
    private final MasterRefreshUseCase useCase;

    public MasterRefreshController(MasterRefreshUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/refresh")
    public List<MasterRefreshOutcomeResponse> refresh() {
        return useCase.refreshAll().stream()
            .map(o -> new MasterRefreshOutcomeResponse(o.masterName(), o.status(), o.recordsUpserted(), o.errorMessage()))
            .toList();
    }

    @GetMapping("/refresh-status")
    public List<MasterStatusResponse> status() {
        return useCase.status().stream()
            .map(s -> new MasterStatusResponse(s.masterName(), s.recordCount(), s.lastRefreshedAt(), s.lastStatus()))
            .toList();
    }

    @GetMapping("/currencies")
    public List<String> currencies() {
        return useCase.availableCurrencies();
    }
}
