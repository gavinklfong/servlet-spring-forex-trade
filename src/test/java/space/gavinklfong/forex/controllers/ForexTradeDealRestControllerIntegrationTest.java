package space.gavinklfong.forex.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import space.gavinklfong.forex.apiclients.ForexRateApiClient;
import space.gavinklfong.forex.dto.ForexRateBookingReq;
import space.gavinklfong.forex.dto.ForexTradeDealReq;
import space.gavinklfong.forex.dto.TradeAction;
import space.gavinklfong.forex.exceptions.ErrorBody;
import space.gavinklfong.forex.models.ForexRateBooking;
import space.gavinklfong.forex.models.ForexTradeDeal;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockserver.mock.OpenAPIExpectation.openAPIExpectation;

@Slf4j
@MockServerTest("server.url=http://localhost:${mockServerPort}")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationtest")
@Tag("IntegrationTest")
public class ForexTradeDealRestControllerIntegrationTest {
	
	// Configure forex rate api client to point to mock api server
    @TestConfiguration
    static class TestContextConfiguration {
    	@Bean
    	@Primary
    	public ForexRateApiClient initializeForexRateApiClient(@Value("${server.url}") String url) {	
    		return new ForexRateApiClient(url);
    	}
    }
        
	private MockServerClient mockServerClient;
	
	WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@DisplayName("submitDeal - Success case")
	@Test
	public void submitDeal() throws Exception {
		
		// Setup request matcher and response using OpenAPI definition
		mockServerClient
	    .upsert(
	        openAPIExpectation("mockapi/getLatestUSDRate.json")
	        .withOperationsAndResponses(Collections.singletonMap("getLatestRates", "200"))  
	    );
		
		// fire request to book rate and verify the response
		ForexRateBookingReq bookingReq = ForexRateBookingReq.builder()
				.customerId(1l)
				.tradeAction(TradeAction.BUY)
				.baseCurrency("GBP")
				.counterCurrency("USD")
				.baseCurrencyAmount(BigDecimal.valueOf(1000))
				.build();

		EntityExchangeResult<ForexRateBooking> result = webTestClient.post()
				.uri("/rates/book")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(bookingReq), ForexRateBookingReq.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody(ForexRateBooking.class)
				.returnResult();
		
		ForexRateBooking rateBooking = result.getResponseBody();

		// construct and trigger trade deal request
		ForexTradeDealReq dealReq = ForexTradeDealReq.builder()
				.tradeAction(rateBooking.getTradeAction())
				.baseCurrency(rateBooking.getBaseCurrency())
				.counterCurrency(rateBooking.getCounterCurrency())
				.rate(rateBooking.getRate())
				.baseCurrencyAmount(rateBooking.getBaseCurrencyAmount())
				.customerId(rateBooking.getCustomerId())
				.rateBookingRef(rateBooking.getBookingRef())
				.build();

		log.debug(dealReq.toString());
		
		webTestClient.post()
		.uri("/deals")
		.contentType(MediaType.APPLICATION_JSON)
		.body(Mono.just(dealReq), ForexTradeDealReq.class)
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().isOk()
		.expectBody(ForexTradeDeal.class);
	}
	
	@DisplayName("submitDeal - Invalid Req")
	@Test
	public void submitDeal_invalidReq() throws Exception {

		// send an empty request
		ForexTradeDealReq req = new ForexTradeDealReq();
		
		webTestClient.post()
		.uri("/deals")
		.contentType(MediaType.APPLICATION_JSON)
		.body(Mono.just(req), ForexTradeDealReq.class)
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().is4xxClientError()
		.expectBody(ErrorBody.class);
	}
	
	@DisplayName("getDeal - Success case")
	@Test
	public void getDeals() throws Exception {

		
		webTestClient.get()
		.uri(uriBuilder -> uriBuilder
				.path("/deals")
				.queryParam("customerId", 1)
				.build()
				)
		.accept(MediaType.APPLICATION_JSON)
		.exchange()
		.expectStatus().isOk();
	}
	
}
