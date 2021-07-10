package space.gavinklfong.forex.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import space.gavinklfong.forex.apiclients.ForexRateApiClient;
import space.gavinklfong.forex.dto.ForexRateBookingReq;
import space.gavinklfong.forex.dto.TradeAction;
import space.gavinklfong.forex.exceptions.UnknownCustomerException;
import space.gavinklfong.forex.models.ForexRateBooking;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockserver.mock.OpenAPIExpectation.openAPIExpectation;

@MockServerTest("server.url=http://localhost:${mockServerPort}")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationtest")
@Tag("IntegrationTest")
class ForexRateRestControllerIntegrationTest {

	// Configure forex rate api client to point to mock api server
    @TestConfiguration
    static class TestContextConfiguration {
    	@Bean
    	@Primary
    	public ForexRateApiClient initializeForexRateApiClient(@Value("${server.url}") String url) {	
    		return new ForexRateApiClient(url);
    	}
    }


	WebTestClient webTestClient;

	private MockServerClient mockServerClient;
	
    @LocalServerPort
    private int port;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	void getLatestRates() throws Exception {
		
		// Setup request matcher and response using OpenAPI definition
		mockServerClient
	    .upsert(
	        openAPIExpectation("mockapi/getLatestRates.json")
	        .withOperationsAndResponses(Collections.singletonMap("getLatestRates", "200"))  
	    );
		
		// fire request to retrieve the latest rates and verify the response
		webTestClient.get()
		.uri("/rates/latest/GBP")
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().isOk();
//		.expectBody()
//		.jsonPath("$").isArray()
//		.jsonPath("$[0].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[0].counterCurrency").isNotEmpty()
//		.jsonPath("$[0].rate").isNumber()
//		.jsonPath("$[1].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[1].counterCurrency").isNotEmpty()
//		.jsonPath("$[1].rate").isNumber()
//		.jsonPath("$[2].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[2].counterCurrency").isNotEmpty()
//		.jsonPath("$[2].rate").isNumber();
	}
	
	@Test
	void bookRate() throws UnknownCustomerException {
		
		// Setup request matcher and response using OpenAPI definition
		mockServerClient
	    .upsert(
	        openAPIExpectation("mockapi/getLatestUSDRate.json")
	        .withOperationsAndResponses(Collections.singletonMap("getLatestRates", "200"))  
	    );
		
		// fire request to book rate and verify the response
		ForexRateBookingReq req = ForexRateBookingReq.builder()
				.customerId(1l)
				.tradeAction(TradeAction.BUY)
				.baseCurrency("GBP")
				.counterCurrency("USD")
				.baseCurrencyAmount(BigDecimal.valueOf(1000))
				.build();

		webTestClient.post()
				.uri("/rates/book")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(req), ForexRateBookingReq.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody(ForexRateBooking.class);
		
	}
	
}
