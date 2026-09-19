package com.thelifestyle.travel.application;

import com.thelifestyle.travel.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.travel.application.port.out.LocalMasterMirrorRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

// The ID-mapping use case future domain code (trip_leg/leg_price_quote
// creation — phased-plan item 2, not built yet) will call before writing a
// row whose FK targets a master: look up the canonical value AND its
// lifestyle_master UUID via integration-service, then mirror a local row
// using that SAME id so the FK can be satisfied without creating an
// independently-numbered duplicate (§17/§20). If integration-service can't
// be reached or has no match, this falls back to whatever local row
// already exists for that natural key (if any) rather than fabricating
// one from nothing.
@Service
public class LocalMasterIdResolver {
    private final LifestyleMasterReadPort readPort;
    private final LocalMasterMirrorRepository mirrorRepository;

    public LocalMasterIdResolver(LifestyleMasterReadPort readPort, LocalMasterMirrorRepository mirrorRepository) {
        this.readPort = readPort;
        this.mirrorRepository = mirrorRepository;
    }

    public Optional<UUID> resolveCityId(String name, String countryCode) {
        var canonical = readPort.findCity(name);
        if (canonical.isPresent()) {
            var city = canonical.get();
            return Optional.of(mirrorRepository.upsertCity(city.id(), city.name(), city.countryCode(), city.latitude(), city.longitude()));
        }
        return mirrorRepository.findCityId(name, countryCode);
    }

    public Optional<UUID> resolveCurrencyId(String isoCode) {
        var canonical = readPort.findCurrency(isoCode);
        if (canonical.isPresent()) {
            var currency = canonical.get();
            return Optional.of(mirrorRepository.upsertCurrency(currency.id(), currency.isoCode(), null));
        }
        return mirrorRepository.findCurrencyId(isoCode);
    }

    public Optional<UUID> resolveTransportModeId(String code) {
        var canonical = readPort.findTransportMode(code);
        if (canonical.isPresent()) {
            var mode = canonical.get();
            return Optional.of(mirrorRepository.upsertTransportMode(mode.id(), mode.code(), mode.label()));
        }
        return mirrorRepository.findTransportModeId(code);
    }
}
