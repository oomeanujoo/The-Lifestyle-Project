package com.thelifestyle.integration.application;

import com.thelifestyle.integration.adapter.out.frankfurter.FrankfurterClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesPostalCodeEntry;
import com.thelifestyle.integration.adapter.out.resilience.ExternalProviderException;
import com.thelifestyle.integration.application.port.out.CityPincodeRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterRepository;
import com.thelifestyle.integration.application.port.out.CodedMasterType;
import com.thelifestyle.integration.application.port.out.LifestyleMasterCityRepository;
import com.thelifestyle.integration.application.port.out.LifestyleMasterCurrencyRepository;
import com.thelifestyle.integration.application.port.out.MasterRefreshLogRepository;
import com.thelifestyle.integration.config.MasterRefreshProperties;
import com.thelifestyle.integration.domain.CityMaster;
import com.thelifestyle.integration.domain.CityPincode;
import com.thelifestyle.integration.domain.CodedMaster;
import com.thelifestyle.integration.domain.CurrencyMaster;
import com.thelifestyle.integration.domain.MasterRefreshOutcome;
import com.thelifestyle.integration.domain.MasterStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The single owner of every lifestyle_master reference table this app
// currently has a real source for (§17/§20): city (GeoNames) and currency
// (Frankfurter) — genuinely refreshed from a live external API.
// transport_mode/bhk_type/service_addon are deliberately NOT "refreshed"
// from anything: they used to be seeded from hardcoded Java Map literals
// (TRANSPORT_MODES/BHK_TYPES/SERVICE_ADDONS), which was itself the kind of
// compiled-in business data this project's "no hardcoded data — everything
// DB-driven" rule (§10) rules out. They're real DB tables in
// lifestyle_master, managed purely through createOrUpdateCoded() below (a
// thin, generic wrapper over CodedMasterRepository.upsert()) — a human (or
// a future admin UI) puts a row in via the API or a reviewed one-time SQL
// seed, Java never asserts what the "right" categories are. See the
// decision log for the target list of what a real seeding/admin workflow
// for these still needs. visa_requirement/municipality/area/locality
// remain unrefreshed for a different, older reason — visa rules have no
// free source and would mean asserting unverified facts (§10); the
// location hierarchy needs data.gov.in, deliberately still deferred
// (§18/§22). A genuine provider failure (GeoNamesClient/FrankfurterClient
// throwing ExternalProviderException) is recorded as an honest FAILED
// outcome with the real reason — never masked as a successful empty
// result, which was the whole point of making those clients throw.
@Service
public class MasterDataRefreshUseCase {
    private static final String SOURCE_GEONAMES = "GeoNames";
    private static final String INDIA_COUNTRY_CODE = "IN";
    private static final String INDIA_PINCODE_MASTER_NAME = "india_pincode";
    private static final int PINCODES_PER_CITY = 20;

    private final GeoNamesClient geoNamesClient;
    private final FrankfurterClient frankfurterClient;
    private final LifestyleMasterCityRepository cityRepository;
    private final LifestyleMasterCurrencyRepository currencyRepository;
    private final CodedMasterRepository codedMasterRepository;
    private final CityPincodeRepository cityPincodeRepository;
    private final MasterRefreshLogRepository masterRefreshLogRepository;
    private final MasterRefreshProperties masters;

    public MasterDataRefreshUseCase(
        GeoNamesClient geoNamesClient,
        FrankfurterClient frankfurterClient,
        LifestyleMasterCityRepository cityRepository,
        LifestyleMasterCurrencyRepository currencyRepository,
        CodedMasterRepository codedMasterRepository,
        CityPincodeRepository cityPincodeRepository,
        MasterRefreshLogRepository masterRefreshLogRepository,
        MasterRefreshProperties masters
    ) {
        this.geoNamesClient = geoNamesClient;
        this.frankfurterClient = frankfurterClient;
        this.cityRepository = cityRepository;
        this.currencyRepository = currencyRepository;
        this.codedMasterRepository = codedMasterRepository;
        this.cityPincodeRepository = cityPincodeRepository;
        this.masterRefreshLogRepository = masterRefreshLogRepository;
        this.masters = masters;
    }

    // The ONE Settings refresh button pulls every master this service has
    // a real external source for, in one call: city (GeoNames), currency
    // (Frankfurter), and Indian postal codes (GeoNames, saved in
    // lifestyle_master.city_pincode — §21 phase L). Order matters:
    // refreshIndianPincodes() reads back the cities refreshCities() just
    // upserted, so it must run after — Java evaluates List.of()'s
    // arguments left to right, so this ordering is guaranteed, not
    // incidental. transport_mode/bhk_type/service_addon are CRUD-managed
    // (see createOrUpdateCoded()), not refreshed — there's nothing
    // external to pull for them, so a "refresh" outcome for them would be
    // theater. municipality/area/locality/pincode need data.gov.in — the
    // credential is confirmed live (§13 decision log, 2026-09-19) but no
    // refresh code exists yet to call it, so there is nothing yet to add
    // here for them.
    public List<MasterRefreshOutcome> refreshAll() {
        return List.of(refreshCities(), refreshCurrencies(), refreshIndianPincodes());
    }

    public List<MasterStatus> status() {
        return masterRefreshLogRepository.statusForAllMasters();
    }

    public List<CityMaster> findCities(String name) {
        return name == null || name.isBlank() ? cityRepository.findAll() : cityRepository.findByName(name);
    }

    public List<String> availableCurrencies() {
        return currencyRepository.findAllIsoCodes();
    }

    // For Travel/Property's ID-mapping resolver — the stable UUID a
    // currency FK must use, not just the code the UI-facing list returns.
    public Optional<CurrencyMaster> findCurrency(String isoCode) {
        return currencyRepository.findByIsoCode(isoCode);
    }

    public List<CodedMaster> findCoded(CodedMasterType type) {
        return codedMasterRepository.findAll(type);
    }

    // The only way a transport-mode/BHK-type/service-addon row is ever
    // created or changed now — no hardcoded Java values anywhere upstream
    // of this. A human via Swagger, or a future admin UI, is the actual
    // source of these masters; this method is a thin, generic pass-through
    // to the DB (CodedMasterRepository.upsert() already keys on `code`, so
    // calling this twice with the same code updates the label rather than
    // duplicating the row).
    public CodedMaster createOrUpdateCoded(CodedMasterType type, String code, String label) {
        return codedMasterRepository.upsert(type, code, label);
    }

    // The Indian-pincode read side for Travel's route-recommendation
    // "via pincode" resolution — a plain lookup, never inventing data if
    // the pincode isn't present.
    public Optional<CityPincode> findPincode(String pincode) {
        return cityPincodeRepository.findByPincode(pincode);
    }

    public List<CityPincode> findPincodesByCity(UUID cityId) {
        return cityPincodeRepository.findByCityId(cityId);
    }

    // The one targeted refresh action asked for: updates lifestyle_master.city
    // (all seed cities, any country) AND lifestyle_master's Indian seed
    // cities' postal codes (lifestyle_master.city_pincode — see
    // CityPincode's comment for why not lifestyle_master.pincode itself), in one
    // call, each step logging its own honest outcome rather than one
    // outcome hiding the other's partial failure.
    public List<MasterRefreshOutcome> refreshIndiaCityAndPincodes() {
        var cityOutcome = refreshCities();
        var pincodeOutcome = refreshIndianPincodes();
        return List.of(cityOutcome, pincodeOutcome);
    }

    // Two sources, both real: the explicit seedCities list (curated
    // international examples like Dubai, looked up by exact name) and a
    // bulk, paged discovery of India's biggest populated places by
    // GeoNames' own population ranking (searchByCountry) — bounded by
    // indiaCityTargetCount/indiaCityPageSize/indiaCityMaxPages so a single
    // refresh can grow past six named seed cities to 100+ real, sourced
    // Indian cities without ever inventing one, and without spending more
    // than pageSize*maxPages GeoNames credits doing it.
    private MasterRefreshOutcome refreshCities() {
        var startedAt = Instant.now();
        if (!geoNamesClient.isConfigured()) {
            var outcome = MasterRefreshOutcome.failed("city", "GEONAMES_USERNAME not configured on integration-service");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        var upserted = 0;
        var misses = 0;
        try {
            for (var cityName : masters.seedCities()) {
                var matches = geoNamesClient.search(cityName, 5);
                if (matches.isEmpty()) {
                    misses++;
                    continue;
                }
                var best = matches.get(0);
                cityRepository.upsert(best.name(), best.countryCode(), parseOrNull(best.lat()), parseOrNull(best.lng()), SOURCE_GEONAMES);
                upserted++;
            }

            var discovered = 0;
            var page = 0;
            while (discovered < masters.indiaCityTargetCount() && page < masters.indiaCityMaxPages()) {
                var startRow = page * masters.indiaCityPageSize();
                var batch = geoNamesClient.searchByCountry(INDIA_COUNTRY_CODE, masters.indiaCityPageSize(), startRow);
                if (batch.isEmpty()) break; // GeoNames ran out of results before hitting the target — real limit, not a bug
                for (var entry : batch) {
                    if (entry.name() == null || entry.name().isBlank()) {
                        misses++;
                        continue;
                    }
                    cityRepository.upsert(entry.name(), entry.countryCode(), parseOrNull(entry.lat()), parseOrNull(entry.lng()), SOURCE_GEONAMES);
                    upserted++;
                    discovered++;
                }
                page++;
            }
        } catch (ExternalProviderException ex) {
            // A genuine GeoNames failure (bad auth, quota exceeded,
            // malformed response) — record it as FAILED for the whole run
            // rather than quietly treating it as "not found," and stop: if
            // GeoNames is rejecting one request it's almost certainly
            // rejecting all of them (e.g. an invalid username), so
            // continuing would just burn quota on calls destined to fail
            // the same way. Whatever upserted successfully before the
            // failure stays in lifestyle_master.city — upsert-and-append
            // never rolls back on a later failure (§17.1).
            var outcome = MasterRefreshOutcome.failed("city", ex.getMessage());
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        var outcome = misses == 0
            ? MasterRefreshOutcome.success("city", upserted)
            : MasterRefreshOutcome.partial("city", upserted, misses + " seed/discovered entrie(s) had no usable name or GeoNames match");
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }

    // Imports every currency Frankfurter returns for the configured base —
    // no pre-configured shortlist to fall short of, so "success" now means
    // "Frankfurter answered with at least the base currency plus whatever
    // it actually supports," not "matched an expected count."
    private MasterRefreshOutcome refreshCurrencies() {
        var startedAt = Instant.now();
        try {
            var response = frankfurterClient.latest(masters.baseCurrency());
            currencyRepository.upsert(masters.baseCurrency(), 1.0);
            var upserted = 1;
            for (var entry : response.rates().entrySet()) {
                currencyRepository.upsert(entry.getKey(), entry.getValue());
                upserted++;
            }

            var outcome = response.rates().isEmpty()
                ? MasterRefreshOutcome.partial("currency", upserted,
                    "Frankfurter returned zero rates for base " + masters.baseCurrency() + " — only the base currency itself was saved")
                : MasterRefreshOutcome.success("currency", upserted);
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        } catch (ExternalProviderException ex) {
            // A genuine Frankfurter failure — network error, malformed
            // response — never masked as "0 rates found."
            var outcome = MasterRefreshOutcome.failed("currency", ex.getMessage());
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }
    }

    // Reads Indian cities straight from lifestyle_master.city — the
    // central master (populated by refreshCities() above, or a previous
    // run of it), never a local duplicate list — and never fetches a city
    // itself, so this can be called on its own once city data exists. Now
    // that refreshCities() can produce 100+ real Indian cities in one run,
    // this is deliberately bounded to indiaPincodeCityLimit rather than
    // looping every one of them: one searchPostalCodes call per city would
    // otherwise spend most of an hourly GeoNames quota on a single button
    // click. For each city in the bounded set, fetches GeoNames' postal
    // codes and saves the trustworthy ones in lifestyle_master.city_pincode
    // (see CityPincode's comment for why not lifestyle_master.pincode
    // itself). A GeoNames result missing a postal code or place name is
    // skipped, not persisted with a fabricated value — that is the concrete
    // meaning of "do not invent municipality, area, or locality values"
    // for this refresh.
    private MasterRefreshOutcome refreshIndianPincodes() {
        var startedAt = Instant.now();
        if (!geoNamesClient.isConfigured()) {
            var outcome = MasterRefreshOutcome.failed(INDIA_PINCODE_MASTER_NAME, "GEONAMES_USERNAME not configured on integration-service");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        var indianCities = cityRepository.findAll().stream()
            .filter(c -> INDIA_COUNTRY_CODE.equalsIgnoreCase(c.countryCode()))
            .limit(masters.indiaPincodeCityLimit())
            .toList();
        if (indianCities.isEmpty()) {
            var outcome = MasterRefreshOutcome.partial(INDIA_PINCODE_MASTER_NAME, 0,
                "no Indian city found in lifestyle_master.city yet — refresh city first");
            masterRefreshLogRepository.record(outcome, startedAt);
            return outcome;
        }

        var upserted = 0;
        var untrustworthy = 0;
        for (var city : indianCities) {
            try {
                var postalMatches = geoNamesClient.searchPostalCodes(city.name(), INDIA_COUNTRY_CODE, PINCODES_PER_CITY);
                for (var p : postalMatches) {
                    if (!isTrustworthy(p)) {
                        untrustworthy++;
                        continue;
                    }
                    cityPincodeRepository.upsert(city.id(),
                        p.postalCode(), p.placeName(), p.adminName2(), p.adminName3(), p.lat(), p.lng(), SOURCE_GEONAMES);
                    upserted++;
                }
            } catch (ExternalProviderException ex) {
                var outcome = MasterRefreshOutcome.failed(INDIA_PINCODE_MASTER_NAME, ex.getMessage());
                masterRefreshLogRepository.record(outcome, startedAt);
                return outcome;
            }
        }

        var outcome = untrustworthy == 0
            ? MasterRefreshOutcome.success(INDIA_PINCODE_MASTER_NAME, upserted)
            : MasterRefreshOutcome.partial(INDIA_PINCODE_MASTER_NAME, upserted,
                untrustworthy + " GeoNames postal-code result(s) skipped — missing a postal code or place name, not a trustworthy match");
        masterRefreshLogRepository.record(outcome, startedAt);
        return outcome;
    }

    // The minimal trust bar: GeoNames must have actually returned a postal
    // code and a place name for this result. Anything less would mean this
    // refresh inventing a value to fill the gap — exactly what it must not
    // do. Coordinates are allowed to be absent (still a real place, just
    // without a usable lat/lon for this pass).
    private static boolean isTrustworthy(GeoNamesPostalCodeEntry entry) {
        return entry.postalCode() != null && !entry.postalCode().isBlank()
            && entry.placeName() != null && !entry.placeName().isBlank();
    }

    private static Double parseOrNull(String value) {
        try {
            return value == null ? null : Double.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
