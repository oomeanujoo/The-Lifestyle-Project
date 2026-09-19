package com.thelifestyle.property.application;

import com.thelifestyle.property.application.port.out.LifestyleMasterReadPort;
import com.thelifestyle.property.application.port.out.LocalMasterMirrorRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

// The ID-mapping use case future domain code (rent_snapshot/cost_estimate
// creation — phased-plan item 2, not built yet) will call before writing a
// row whose FK targets a master: look up the canonical value AND its
// lifestyle_master UUID via integration-service, then mirror a local row
// using that SAME id so the FK can be satisfied without creating an
// independently-numbered duplicate (§17/§20). Falls back to whatever local
// row already exists for that natural key if integration-service can't be
// reached or has no match.
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

    public Optional<UUID> resolveBhkTypeId(String code) {
        var canonical = readPort.findBhkType(code);
        if (canonical.isPresent()) {
            var bhkType = canonical.get();
            return Optional.of(mirrorRepository.upsertBhkType(bhkType.id(), bhkType.code(), bhkType.label()));
        }
        return mirrorRepository.findBhkTypeId(code);
    }

    public Optional<UUID> resolveServiceAddonId(String code) {
        var canonical = readPort.findServiceAddon(code);
        if (canonical.isPresent()) {
            var addon = canonical.get();
            return Optional.of(mirrorRepository.upsertServiceAddon(addon.id(), addon.code(), addon.label()));
        }
        return mirrorRepository.findServiceAddonId(code);
    }
}
