package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.application.MasterDataRefreshUseCase;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.domain.CityPincode;
import com.thelifestyle.integration.domain.CodedMaster;
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

// THE authoritative master-data API (§17/§20) — the one place
// travel-service/property-service should request or read city, currency,
// transport-mode, bhk-type, and service-addon masters, instead of owning
// any of that themselves. Deliberately under /masters/lifestyle/** —
// separate from MasterLookupController's raw-passthrough diagnostic
// endpoints (unchanged, hidden from the public Swagger contract — see that
// class). Not yet called by travel-service/property-service in this pass —
// see the decision log for exactly what's wired and what's still pending.
@Tag(name = "Master Data", description = "The single source of truth for lifestyle_master — city, currency, transport mode, BHK type, service add-on.")
@RestController
@RequestMapping("/api/integration/v1/masters/lifestyle")
public class MasterDataController {
    private final MasterDataRefreshUseCase useCase;

    public MasterDataController(MasterDataRefreshUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/refresh")
    public List<MasterRefreshOutcomeResponse> refresh() {
        return useCase.refreshAll().stream()
            .map(o -> new MasterRefreshOutcomeResponse(o.masterName(), o.status(), o.recordsUpserted(), o.errorMessage()))
            .toList();
    }

    // The one targeted refresh action: lifestyle_master.city (all seed
    // cities) plus the Indian seed cities' postal codes, saved in
    // lifestyle_master.city_pincode — see MasterDataRefreshUseCase and
    // CityPincode's own comments for why pincodes don't go into
    // lifestyle_master.pincode itself. Deliberately separate from the broad
    // /refresh above, which also touches currency/transport_mode/bhk_type/
    // service_addon — this endpoint is scoped to exactly the two masters
    // the route-recommendation feature depends on.
    @PostMapping("/refresh/india-geo")
    public List<MasterRefreshOutcomeResponse> refreshIndiaGeo() {
        return useCase.refreshIndiaCityAndPincodes().stream()
            .map(o -> new MasterRefreshOutcomeResponse(o.masterName(), o.status(), o.recordsUpserted(), o.errorMessage()))
            .toList();
    }

    @GetMapping("/refresh-status")
    public List<MasterStatusResponse> status() {
        return useCase.status().stream()
            .map(s -> new MasterStatusResponse(s.masterName(), s.recordCount(), s.lastRefreshedAt(), s.lastStatus(),
                s.source(), s.failureReason()))
            .toList();
    }

    @GetMapping("/cities")
    public List<CityMasterResponse> cities(@RequestParam(name = "name", required = false) String name) {
        return useCase.findCities(name).stream()
            .map(c -> new CityMasterResponse(c.id().toString(), c.name(), c.countryCode(), c.latitude(), c.longitude(), c.source(), c.updatedAt()))
            .toList();
    }

    // GeoNames Indian postal-code data (lifestyle_master.city_pincode) —
    // for Travel's route-recommendation "via pincode" resolution. Not
    // lifestyle_master.pincode itself; see CityPincode's own comment for why.
    @GetMapping("/pincodes")
    public List<PincodeMasterResponse> pincodesByCity(@RequestParam(name = "cityId") String cityId) {
        return useCase.findPincodesByCity(UUID.fromString(cityId)).stream()
            .map(this::toPincodeResponse)
            .toList();
    }

    @GetMapping("/pincodes/{code}")
    public ResponseEntity<PincodeMasterResponse> pincode(@PathVariable String code) {
        return useCase.findPincode(code)
            .map(this::toPincodeResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private PincodeMasterResponse toPincodeResponse(CityPincode p) {
        return new PincodeMasterResponse(p.cityId().toString(), p.cityName(), p.countryCode(),
            p.pincode(), p.placeName(), p.adminName2(), p.adminName3(), p.latitude(), p.longitude(), p.source(), p.capturedAt());
    }

    @GetMapping("/currencies")
    public List<String> currencies() {
        return useCase.availableCurrencies();
    }

    // Not consumed by the UI (that's the bare-string /currencies list above) —
    // this is for Travel/Property's ID-mapping resolver, which needs the
    // stable authoritative UUID a currency FK must use, not just its code.
    @GetMapping("/currencies/{isoCode}")
    public ResponseEntity<CurrencyMasterResponse> currency(@PathVariable String isoCode) {
        return useCase.findCurrency(isoCode)
            .map(c -> new CurrencyMasterResponse(c.id().toString(), c.isoCode()))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/transport-modes")
    public List<CodedMasterResponse> transportModes() {
        return codedResponses(CodedMasterType.TRANSPORT_MODE);
    }

    @GetMapping("/bhk-types")
    public List<CodedMasterResponse> bhkTypes() {
        return codedResponses(CodedMasterType.BHK_TYPE);
    }

    @GetMapping("/service-addons")
    public List<CodedMasterResponse> serviceAddons() {
        return codedResponses(CodedMasterType.SERVICE_ADDON);
    }

    // These three POST endpoints are now the ONLY way a transport-mode/
    // BHK-type/service-addon row is created or changed — no Java Map of
    // "the real values" exists anywhere upstream of this anymore (see
    // MasterDataRefreshUseCase's class comment). A human via this Swagger
    // page, or a future admin UI, is the actual source of these masters.
    @PostMapping("/transport-modes")
    public CodedMasterResponse createTransportMode(@RequestBody CodedMasterRequest request) {
        return toResponse(useCase.createOrUpdateCoded(CodedMasterType.TRANSPORT_MODE, request.code(), request.label()));
    }

    @PostMapping("/bhk-types")
    public CodedMasterResponse createBhkType(@RequestBody CodedMasterRequest request) {
        return toResponse(useCase.createOrUpdateCoded(CodedMasterType.BHK_TYPE, request.code(), request.label()));
    }

    @PostMapping("/service-addons")
    public CodedMasterResponse createServiceAddon(@RequestBody CodedMasterRequest request) {
        return toResponse(useCase.createOrUpdateCoded(CodedMasterType.SERVICE_ADDON, request.code(), request.label()));
    }

    private List<CodedMasterResponse> codedResponses(CodedMasterType type) {
        return useCase.findCoded(type).stream()
            .map(this::toResponse)
            .toList();
    }

    private CodedMasterResponse toResponse(CodedMaster m) {
        return new CodedMasterResponse(m.id().toString(), m.code(), m.label());
    }
}
