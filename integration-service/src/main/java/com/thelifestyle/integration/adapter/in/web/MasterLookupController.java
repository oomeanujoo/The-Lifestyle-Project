package com.thelifestyle.integration.adapter.in.web;

import com.thelifestyle.integration.adapter.out.datagovin.DataGovInClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesClient;
import com.thelifestyle.integration.adapter.out.geonames.GeoNamesEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Manual-trigger endpoints for the external master-data sources — there is
// no scheduled refresh job calling these yet (that job lives in
// travel-service/property-service against their own master tables, which
// don't exist locally either — §17), so these exist to prove each client
// actually reaches its API and parses a real response, callable by hand
// while that job doesn't exist.
@RestController
@RequestMapping("/api/integration/v1/masters")
public class MasterLookupController {
    private final GeoNamesClient geoNamesClient;
    private final DataGovInClient dataGovInClient;

    public MasterLookupController(GeoNamesClient geoNamesClient, DataGovInClient dataGovInClient) {
        this.geoNamesClient = geoNamesClient;
        this.dataGovInClient = dataGovInClient;
    }

    @GetMapping("/geonames/search")
    public List<GeoNamesEntry> geoNamesSearch(@RequestParam("q") String query) {
        return geoNamesClient.search(query, 10);
    }

    @GetMapping("/datagovin/sample")
    public List<Map<String, Object>> dataGovInSample() {
        return dataGovInClient.fetchSample(10);
    }
}
