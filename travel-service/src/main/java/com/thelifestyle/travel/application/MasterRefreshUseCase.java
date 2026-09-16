package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.CityMasterRepository;
import com.thelifestyle.travel.application.port.out.CurrencyMasterRepository;
import com.thelifestyle.travel.application.port.out.ExternalMasterDataPort;
import com.thelifestyle.travel.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.travel.application.port.out.TransportModeMasterRepository;
import com.thelifestyle.travel.config.IntegrationProperties;
import com.thelifestyle.travel.domain.MasterRefreshOutcome;
import com.thelifestyle.travel.domain.MasterStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// The "first refresh" job (§17): pulls the city master from GeoNames and
// the currency master from Frankfurter — both via integration-service,
// never directly — and upserts each into Postgres, never deleting an
// existing row (§17.1). transport_mode is a different kind of master: a
// fixed, closed set of categorical labels (flight/train/road) with no
// external source at all — there's nothing to "fetch," so it's seeded
// directly here rather than routed through integration-service, and its
// outcome is always SUCCESS (no network call to fail). visa_requirement
// deliberately has no seed here — unlike transport labels, visa rules are
// asserted facts, and inventing placeholder ones would violate the
// AI/app-never-asserts-an-unverified-fact rule (§10) even as "seed data."
// Every master's outcome is logged even on failure, so nothing here
// silently does nothing. Which cities/currencies to look up is
// configuration (application.yaml/env vars), never baked into this class.
@Service
public class MasterRefreshUseCase {
    private static final Map<String, String> TRANSPORT_MODES = Map.of(
        "flight", "Flight",
        "train", "Train",
        "road", "Road"
    );

    private final ExternalMasterDataPort externalMasterDataPort;
    private final CityMasterRepository cityMasterRepository;
    private final CurrencyMasterRepository currencyMasterRepository;
    private final TransportModeMasterRepository transportModeMasterRepository;
    private final MasterRefreshLogRepository masterRefreshLogRepository;
    private final IntegrationProperties.Masters masters;

    public MasterRefreshUseCase(
        ExternalMasterDataPort externalMasterDataPort,
        CityMasterRepository cityMasterRepository,
        CurrencyMasterRepository currencyMasterRepository,
        TransportModeMasterRepository transportModeMasterRepository,
        MasterRefreshLogRepository masterRefreshLogRepository,
        IntegrationProperties integrationProperties
    ) {
        this.externalMasterDataPort = externalMasterDataPort;
        this.cityMasterRepository = cityMasterRepository;
        this.currencyMasterRepository = currencyMasterRepository;
        this.transportModeMasterRepository = transportModeMasterRepository;
        this.masterRefreshLogRepository = masterRefreshLogRepository;
        this.masters = integrationProperties.masters();
    }

    public List<MasterRefreshOutcome> refreshAll() {
        return List.of(refreshCities(), refreshCurrencies(), refreshTransportModes());
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

    // No external call, no network dependency — always succeeds, so this
    // one is unaffected by whatever's happening with integration-service.
    private MasterRefreshOutcome refreshTransportModes() {
        var startedAt = Instant.now();
        for (var entry : TRANSPORT_MODES.entrySet()) {
            transportModeMasterRepository.upsert(entry.getKey(), entry.getValue());
        }
        var outcome = MasterRefreshOutcome.success("transport_mode", TRANSPORT_MODES.size());
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }
}
