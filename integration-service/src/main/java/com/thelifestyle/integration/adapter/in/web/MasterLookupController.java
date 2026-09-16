package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.adapter.out.datagovin.DataGovInClient;
import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterClient;
import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterRates;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesEntry;
import com.thelifestyle.integration.adapter.out.resilience.HealthStatusRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// GeoNames and Frankfurter here are called directly by travel-service and
// property-service's MasterRefreshUseCase (§17/§18) — this is genuinely
// live-called now, not just a manual debug endpoint. data.gov.in's
// datagovin/sample endpoint remains manual-trigger only: its dataset's
// exact response schema hasn't been inspected against a real key/response
// yet, so no refresh job consumes it — see TECHNICAL_ARCHITECTURE.md §18.
@RestController
@RequestMapping("/api/integration/v1/masters")
public class MasterLookupController {
    private static final List<String> EXTERNAL_DATA_CLIENTS = List.of("geonames", "frankfurter", "datagovin");

    private final GeoNamesClient geoNamesClient;
    private final DataGovInClient dataGovInClient;
    private final FrankfurterClient frankfurterClient;
    private final HealthStatusRegistry healthStatusRegistry;

    public MasterLookupController(GeoNamesClient geoNamesClient, DataGovInClient dataGovInClient,
                                  FrankfurterClient frankfurterClient, HealthStatusRegistry healthStatusRegistry) {
        this.geoNamesClient = geoNamesClient;
        this.dataGovInClient = dataGovInClient;
        this.frankfurterClient = frankfurterClient;
        this.healthStatusRegistry = healthStatusRegistry;
    }

    @GetMapping("/geonames/search")
    public List<GeoNamesEntry> geoNamesSearch(@RequestParam("q") String query) {
        return geoNamesClient.search(query, 10);
    }

    @GetMapping("/datagovin/sample")
    public List<Map<String, Object>> dataGovInSample() {
        return dataGovInClient.fetchSample(10);
    }

    @GetMapping("/frankfurter/latest")
    public FrankfurterRates frankfurterLatest(@RequestParam("base") String base, @RequestParam("symbols") String symbols) {
        return frankfurterClient.latest(base, symbols);
    }

    // Same "decided on the last real hit" health model as
    // AiProviderStatusController — never a synthetic ping. `configured`
    // reflects each client's own isConfigured() check — Frankfurter needs
    // no credential at all, so it's always true; the other two aren't.
    @GetMapping("/health")
    public List<AiProviderStatusResponse> health() {
        return EXTERNAL_DATA_CLIENTS.stream()
            .map(name -> {
                var health = healthStatusRegistry.get(name);
                var configured = switch (name) {
                    case "geonames" -> geoNamesClient.isConfigured();
                    case "datagovin" -> dataGovInClient.isConfigured();
                    default -> true; // frankfurter needs no credential
                };
                return new AiProviderStatusResponse(
                    name, configured, health.status().name(),
                    health.lastCheckedAt() == null ? null : health.lastCheckedAt().toString(),
                    health.lastError());
            })
            .toList();
    }
}
