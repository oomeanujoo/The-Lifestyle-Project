package com.thelifestyle.integration.adapter.out.frankfurter;

import com.thelifestyle.integration.adapter.out.resilience.ExternalProviderException;
import com.thelifestyle.integration.adapter.out.resilience.HealthStatusRegistry;
import com.thelifestyle.integration.adapter.out.resilience.ResilienceGuard;
import com.thelifestyle.integration.config.ExternalApisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FrankfurterClientTest {

    private static ExternalApisProperties properties() {
        var unconfiguredProvider = new ExternalApisProperties.Ai.Provider("", "", "", 25, 60000);
        var unconfiguredLocal = new ExternalApisProperties.Ai.Local("", "", 720, 60, 60000);
        return new ExternalApisProperties(
            new ExternalApisProperties.GeoNames("", "http://fake-geonames.test", 10, 60000),
            new ExternalApisProperties.DataGovIn("", "5c2f62fe-5afa-4119-a499-fec9d604d5bd", "https://api.data.gov.in", 10, 60000),
            new ExternalApisProperties.Frankfurter("http://fake-frankfurter.test", 20, 60000),
            new ExternalApisProperties.Ai(unconfiguredLocal, unconfiguredProvider, unconfiguredProvider));
    }

    @Test void returnsRealRatesOnSuccess() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/latest")))
            .andRespond(withSuccess("""
                {"base":"INR","date":"2026-09-17","rates":{"USD":0.01042,"EUR":0.00903}}
                """, MediaType.APPLICATION_JSON));

        var client = new FrankfurterClient(properties(), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);
        var result = client.latest("INR");

        assertEquals(2, result.rates().size());
        server.verify();
    }

    @Test void throwsExplicitlyOnServerError() {
        // A real network/server failure must never be mistaken for "rates
        // successfully came back empty" — this is what used to happen
        // when ResilienceGuard.call() swallowed the exception into a null
        // fallback.
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/latest")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        var client = new FrankfurterClient(properties(), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.latest("INR"));
        server.verify();
    }

    @Test void throwsExplicitlyOnMalformedResponse() {
        var restClientBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(containsString("/latest")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var client = new FrankfurterClient(properties(), new ResilienceGuard(new HealthStatusRegistry()), restClientBuilder);

        assertThrows(ExternalProviderException.class, () -> client.latest("INR"));
        server.verify();
    }
}
