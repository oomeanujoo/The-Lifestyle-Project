package com.thelifestyle.property.application;

import com.thelifestyle.property.application.port.out.BhkTypeMasterRepository;
import com.thelifestyle.property.application.port.out.CityMasterRepository;
import com.thelifestyle.property.application.port.out.CurrencyMasterRepository;
import com.thelifestyle.property.application.port.out.ExternalMasterDataPort;
import com.thelifestyle.property.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.property.application.port.out.ServiceAddonMasterRepository;
import com.thelifestyle.property.config.IntegrationProperties;
import com.thelifestyle.property.domain.MasterRefreshOutcome;
import com.thelifestyle.property.domain.MasterStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// The "first refresh" job (§17): pulls the city master from GeoNames and
// the currency master from Frankfurter — both via integration-service,
// never directly — and upserts each into Postgres, never deleting an
// existing row (§17.1). bhk_type/service_addon are a different kind of
// master: fixed, closed sets of categorical labels with no external
// source at all — nothing to "fetch," so they're seeded directly here,
// always succeeding regardless of integration-service's reachability.
// Every master's outcome is logged even on failure, so nothing here
// silently does nothing. Which cities/currencies to look up is
// configuration (application.yaml/env vars), never baked into this class.
// Property's deeper area/locality/pincode masters are deliberately not
// refreshed here yet — data.gov.in's exact response schema hasn't been
// inspected against a live call, and guessing it wrong risks silently
// corrupting the hierarchy.
@Service
public class MasterRefreshUseCase {
    private static final Map<String, String> BHK_TYPES = Map.of(
        "1RK", "1 RK",
        "1BHK", "1 BHK",
        "2BHK", "2 BHK",
        "3BHK", "3 BHK",
        "4BHK", "4 BHK",
        "Villa", "Villa"
    );
    private static final Map<String, String> SERVICE_ADDONS = Map.of(
        "tiffin", "Tiffin / meal service",
        "gym", "Gym / fitness membership"
    );

    private final ExternalMasterDataPort externalMasterDataPort;
    private final CityMasterRepository cityMasterRepository;
    private final CurrencyMasterRepository currencyMasterRepository;
    private final BhkTypeMasterRepository bhkTypeMasterRepository;
    private final ServiceAddonMasterRepository serviceAddonMasterRepository;
    private final MasterRefreshLogRepository masterRefreshLogRepository;
    private final IntegrationProperties.Masters masters;

    public MasterRefreshUseCase(
        ExternalMasterDataPort externalMasterDataPort,
        CityMasterRepository cityMasterRepository,
        CurrencyMasterRepository currencyMasterRepository,
        BhkTypeMasterRepository bhkTypeMasterRepository,
        ServiceAddonMasterRepository serviceAddonMasterRepository,
        MasterRefreshLogRepository masterRefreshLogRepository,
        IntegrationProperties integrationProperties
    ) {
        this.externalMasterDataPort = externalMasterDataPort;
        this.cityMasterRepository = cityMasterRepository;
        this.currencyMasterRepository = currencyMasterRepository;
        this.bhkTypeMasterRepository = bhkTypeMasterRepository;
        this.serviceAddonMasterRepository = serviceAddonMasterRepository;
        this.masterRefreshLogRepository = masterRefreshLogRepository;
        this.masters = integrationProperties.masters();
    }

    public List<MasterRefreshOutcome> refreshAll() {
        return List.of(refreshCities(), refreshCurrencies(), refreshBhkTypes(), refreshServiceAddons());
    }

    public List<MasterStatus> status() {
        return masterRefreshLogRepository.statusForAllMasters();
    }

    // Backs the Settings page's currency selector — options come from
    // whatever's actually in the database right now, never a hardcoded list.
    public List<String> availableCurrencies() {
        return currencyMasterRepository.findAllIsoCodes();
    }

    private MasterRefreshOutcome refreshCities() {
        var startedAt = Instant.now();
        var upserted = 0;
        var misses = 0;

        for (var cityName : masters.seedCities()) {
            var found = externalMasterDataPort.findCity(cityName);
            if (found.isPresent()) {
                var city = found.get();
                cityMasterRepository.upsert(city.name(), city.countryCode(), city.latitude(), city.longitude());
                upserted++;
            } else {
                misses++;
            }
        }

        var outcome = misses == 0
            ? MasterRefreshOutcome.success("city", upserted)
            : MasterRefreshOutcome.partial("city", upserted, misses + " of " + masters.seedCities().size()
                + " seed cities returned no match — check integration-service's own logs: this means either GeoNames genuinely had no match, or travel-service/property-service couldn't reach integration-service at all (see IntegrationServiceClient's WARN logs)");
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }

    private MasterRefreshOutcome refreshCurrencies() {
        var startedAt = Instant.now();
        try {
            var rates = externalMasterDataPort.fetchExchangeRates(masters.baseCurrency(), masters.trackedCurrencies());
            currencyMasterRepository.upsert(masters.baseCurrency(), 1.0);
            var upserted = 1;
            for (var entry : rates.entrySet()) {
                currencyMasterRepository.upsert(entry.getKey(), entry.getValue());
                upserted++;
            }

            var outcome = rates.size() == masters.trackedCurrencies().size()
                ? MasterRefreshOutcome.success("currency", upserted)
                : MasterRefreshOutcome.partial("currency", upserted, "Frankfurter returned " + rates.size() + " of " + masters.trackedCurrencies().size() + " tracked currencies");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        } catch (Exception ex) {
            var outcome = MasterRefreshOutcome.failed("currency", ex.getMessage());
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }
    }

    // No external call, no network dependency — always succeeds, so these
    // two are unaffected by whatever's happening with integration-service.
    private MasterRefreshOutcome refreshBhkTypes() {
        var startedAt = Instant.now();
        for (var entry : BHK_TYPES.entrySet()) {
            bhkTypeMasterRepository.upsert(entry.getKey(), entry.getValue());
        }
        var outcome = MasterRefreshOutcome.success("bhk_type", BHK_TYPES.size());
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }

    private MasterRefreshOutcome refreshServiceAddons() {
        var startedAt = Instant.now();
        for (var entry : SERVICE_ADDONS.entrySet()) {
            serviceAddonMasterRepository.upsert(entry.getKey(), entry.getValue());
        }
        var outcome = MasterRefreshOutcome.success("service_addon", SERVICE_ADDONS.size());
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }
}
