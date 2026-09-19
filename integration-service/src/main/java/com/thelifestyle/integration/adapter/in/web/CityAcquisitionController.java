package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.adapter.out.resilience.ExternalProviderException;
import com.thelifestyle.integration.application.CityAcquisitionUseCase;
import com.thelifestyle.integration.domain.CityMaster;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master Data", description = "Verified lifestyle master acquisition")
@RestController
@RequestMapping("/api/integration/v1/masters/lifestyle/cities")
public class CityAcquisitionController {
    private final CityAcquisitionUseCase useCase;

    public CityAcquisitionController(CityAcquisitionUseCase useCase) {
        this.useCase = useCase;
    }

    // Explicit POST avoids provider calls on every typeahead keystroke.
    // A 404 leaves the master unchanged; the separate AI suggestion API
    // can offer DRAFT ideas, but cannot create an authoritative city.
    @PostMapping("/acquire")
    public ResponseEntity<CityMasterResponse> acquire(
        @RequestParam("name") String name,
        @RequestParam(name = "countryCode", defaultValue = "IN") String countryCode
    ) {
        return useCase.acquire(name, countryCode)
            .map(CityAcquisitionController::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ExternalProviderException.class)
    public ResponseEntity<ApiErrorResponse> providerFailure(ExternalProviderException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiErrorResponse(ex.getMessage()));
    }

    private static CityMasterResponse toResponse(CityMaster city) {
        return new CityMasterResponse(city.id().toString(), city.name(), city.countryCode(),
            city.latitude(), city.longitude(), city.source(), city.updatedAt());
    }
}
