package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.AuditLogRepository;
import com.thelifestyle.travel.application.port.out.LegPriceQuoteRepository;
import com.thelifestyle.travel.application.port.out.TripLegRepository;
import com.thelifestyle.travel.domain.LegPriceQuote;
import com.thelifestyle.travel.domain.LegPriceQuoteOutcome;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

// Manual price entry (§17, §19) — no free live fare source exists for
// flights/trains/road, so a price quote is always a person typing in what
// they found, never fetched automatically. `confidence` defaults to
// 'UNVERIFIED' (matching leg_price_quote's own DB default) unless the
// caller explicitly says otherwise — never asserted as verified by this
// code. currency is resolved the same way TripLegUseCase resolves
// fromCity/toCity/transportMode: by name, against the refreshed
// lifestyle_master masters, never a raw id from the caller.
@Service
public class LegPriceQuoteUseCase {
    private static final String DEFAULT_CONFIDENCE = "UNVERIFIED";

    private static final String ENTITY_TYPE = "leg_price_quote";

    private final LegPriceQuoteRepository legPriceQuoteRepository;
    private final TripLegRepository tripLegRepository;
    private final LocalMasterIdResolver masterIdResolver;
    private final AuditLogRepository auditLogRepository;

    public LegPriceQuoteUseCase(LegPriceQuoteRepository legPriceQuoteRepository, TripLegRepository tripLegRepository,
                                 LocalMasterIdResolver masterIdResolver, AuditLogRepository auditLogRepository) {
        this.legPriceQuoteRepository = legPriceQuoteRepository;
        this.tripLegRepository = tripLegRepository;
        this.masterIdResolver = masterIdResolver;
        this.auditLogRepository = auditLogRepository;
    }

    public LegPriceQuoteOutcome create(UUID tripLegId, String currency, double amount, Double durationHours,
                                        String source, String sourceUrl, String confidence) {
        if (tripLegRepository.findById(tripLegId).isEmpty()) {
            return LegPriceQuoteOutcome.error("trip leg '" + tripLegId + "' does not exist");
        }
        if (!StringUtils.hasText(currency)) {
            return LegPriceQuoteOutcome.error("currency is required");
        }
        if (amount < 0) {
            return LegPriceQuoteOutcome.error("amount must be zero or greater");
        }
        if (durationHours != null && durationHours < 0) {
            return LegPriceQuoteOutcome.error("durationHours must be zero or greater");
        }
        if (!StringUtils.hasText(source)) {
            return LegPriceQuoteOutcome.error("source is required — say where this price came from");
        }

        var currencyId = masterIdResolver.resolveCurrencyId(currency.trim());
        if (currencyId.isEmpty()) {
            return LegPriceQuoteOutcome.error(
                "currency '" + currency + "' was not found — it must exist in lifestyle_master.currency first");
        }

        var resolvedConfidence = StringUtils.hasText(confidence) ? confidence.trim().toUpperCase() : DEFAULT_CONFIDENCE;
        var quote = legPriceQuoteRepository.create(tripLegId, currencyId.get(), amount, durationHours,
            source.trim(), sourceUrl, resolvedConfidence);
        auditLogRepository.record(ENTITY_TYPE, quote.id(), "CREATE");
        return LegPriceQuoteOutcome.ok(quote);
    }

    public List<LegPriceQuote> findByTripLeg(UUID tripLegId) {
        return legPriceQuoteRepository.findByTripLeg(tripLegId);
    }
}
