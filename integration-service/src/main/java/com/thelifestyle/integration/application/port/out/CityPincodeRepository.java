package com.thelifestyle.integration.application.port.out;

import com.thelifestyle.integration.domain.CityPincode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Store for GeoNames Indian postal-code results, keyed by (cityId, pincode)
// — now lifestyle_master.city_pincode (see CityPincode's own comment for
// why this is a same-shaped sibling table there, not lifestyle_master.pincode
// itself). `cityName`/`countryCode` are deliberately not upsert parameters
// here: the new table only stores `city_id`, a real FK into
// lifestyle_master.city, so name/country are always read back via a join,
// never duplicated into this table. Upsert-and-append only (§17.1), same as
// every other master here — never a delete.
public interface CityPincodeRepository {
    void upsert(UUID cityId, String pincode, String placeName,
                String adminName2, String adminName3, Double latitude, Double longitude, String source);
    List<CityPincode> findByCityId(UUID cityId);
    Optional<CityPincode> findByPincode(String pincode);
    long count();
}
