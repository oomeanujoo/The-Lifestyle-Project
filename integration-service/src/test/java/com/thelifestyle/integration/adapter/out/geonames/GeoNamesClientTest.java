package com.thelifestyle.integration.adapter.out.geonames;

import com.thelifestyle.integration.adapter.out.resilience.ExternalProviderException;
import com.thelifestyle.integration.adapter.out.resilience.HealthStatusRegistry;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Uses MockRestServiceServer (bundled with spring-boot-starter-test, no new
// dependency) rather than hitting the real GeoNames API — proves the
// explicit-failure behavior (§18, this pass) without any network call or
// real credential.
class GeoNamesClientTest {

    private static ExternalApisProperties propertiesWith(String username) {
        var unconfiguredProvider = new ExternalApisProperties.Ai.Provider("", "", "", 25, 60000);
        var unconfiguredLocal = new ExternalApisProperties.Ai.Local("", "", 720, 60, 60000);
        return new ExternalApisProperties(
            new ExternalApisProperties.GeoNames(username, "http://fake-geonames.test", 10, 60000),
            new ExternalApisProperties.DataGovIn("", "5c2f62fe-5afa-4119-a499-fec9d604d5bd", "https://api.data.gov.in", 10, 60000),
            new ExternalApisProperties.Frankfurter("https://api.frankfurter.dev/v1", 20, 60000),
            new ExternalApisProperties.Ai(unconfiguredLocal, unconfiguredProvider, unconfiguredProvider));
    }

    @Test void skipsCallEntirelyWhenNotConfigured() {
        var restClientBuilder = RestClient.builder();
        var client = new GeoNamesClient(propertiesWith(""), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        var result = client.search("Pune", 5);

        assertTrue(result.isEmpty());
    }

    @Test void returnsRealMatchesOnSuccess() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/searchJSON")))
            .andRespond(withSuccess("""
                {"geonames":[{"geonameId":1,"name":"Pune","countryName":"India","countryCode":"IN","adminName1":"Maharashtra","lat":"18.52043","lng":"73.85674"}]}
                """, MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("testuser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);
        var result = client.search("Pune", 5);

        assertEquals(1, result.size());
        assertEquals("Pune", result.get(0).name());
        server.verify();
    }

    @Test void throwsExplicitlyOnGeoNamesErrorShapedResponse() {
        // GeoNames' real quirk: auth failures, exceeded quotas, and bad
        // parameters all come back as HTTP 200 with a `status` object, not
        // an HTTP error. Before this pass, this looked identical to
        // "searched, found nothing" — that's the exact bug being closed.
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/searchJSON")))
            .andRespond(withSuccess("""
                {"status":{"message":"the given user does not exist","value":10}}
                """, MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("baduser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.search("Pune", 5));
        server.verify();
    }

    @Test void throwsExplicitlyOnMalformedResponse() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/searchJSON")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("testuser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.search("Pune", 5));
        server.verify();
    }

    @Test void searchByCountryReturnsRealMatchesOrderedByPopulation() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/searchJSON")))
            .andExpect(requestTo(containsString("country=IN")))
            .andExpect(requestTo(containsString("featureClass=P")))
            .andExpect(requestTo(containsString("orderby=population")))
            .andRespond(withSuccess("""
                {"geonames":[
                    {"geonameId":1,"name":"Mumbai","countryName":"India","countryCode":"IN","adminName1":"Maharashtra","lat":"19.07283","lng":"72.88261"},
                    {"geonameId":2,"name":"Delhi","countryName":"India","countryCode":"IN","adminName1":"Delhi","lat":"28.65195","lng":"77.23149"}
                ]}
                """, MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("testuser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);
        var result = client.searchByCountry("IN", 100, 0);

        assertEquals(2, result.size());
        assertEquals("Mumbai", result.get(0).name());
        server.verify();
    }

    @Test void throwsExplicitlyOnCountrySearchErrorShapedResponse() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/searchJSON")))
            .andRespond(withSuccess("""
                {"status":{"message":"the given user does not exist","value":10}}
                """, MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("baduser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.searchByCountry("IN", 100, 0));
        server.verify();
    }

    @Test void skipsCountrySearchEntirelyWhenNotConfigured() {
        var restClientBuilder = RestClient.builder();
        var client = new GeoNamesClient(propertiesWith(""), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        var result = client.searchByCountry("IN", 100, 0);

        assertTrue(result.isEmpty());
    }

    @Test void skipsPostalCodeSearchEntirelyWhenNotConfigured() {
        var restClientBuilder = RestClient.builder();
        var client = new GeoNamesClient(propertiesWith(""), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        var result = client.searchPostalCodes("Pune", "IN", 20);

        assertTrue(result.isEmpty());
    }

    @Test void returnsRealPostalCodeMatchesOnSuccess() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/postalCodeSearchJSON")))
            .andRespond(withSuccess("""
                {"postalCodes":[{"postalCode":"411001","placeName":"Pune","adminName1":"Maharashtra","adminName2":"Pune","adminName3":null,"lat":18.52,"lng":73.85,"countryCode":"IN"}]}
                """, MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("testuser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);
        var result = client.searchPostalCodes("Pune", "IN", 20);

        assertEquals(1, result.size());
        assertEquals("411001", result.get(0).postalCode());
        assertEquals("Pune", result.get(0).placeName());
        server.verify();
    }

    @Test void throwsExplicitlyOnPostalCodeErrorShapedResponse() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/postalCodeSearchJSON")))
            .andRespond(withSuccess("""
                {"status":{"message":"the given user does not exist","value":10}}
                """, MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("baduser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.searchPostalCodes("Pune", "IN", 20));
        server.verify();
    }

    @Test void throwsExplicitlyOnMalformedPostalCodeResponse() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/postalCodeSearchJSON")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var client = new GeoNamesClient(propertiesWith("testuser"), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.searchPostalCodes("Pune", "IN", 20));
        server.verify();
    }
}
