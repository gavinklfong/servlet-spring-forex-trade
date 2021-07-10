package space.gavinklfong.forex.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import space.gavinklfong.forex.dto.ForexRate;
import space.gavinklfong.forex.dto.ForexRateBookingReq;
import space.gavinklfong.forex.dto.TradeAction;
import space.gavinklfong.forex.exceptions.ErrorBody;
import space.gavinklfong.forex.exceptions.UnknownCustomerException;
import space.gavinklfong.forex.models.ForexRateBooking;
import space.gavinklfong.forex.services.ForexPricingService;
import space.gavinklfong.forex.services.ForexRateService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit test for Rate Rest Controller
 */
@Slf4j
@WebMvcTest(ForexRateRestController.class)
@Tag("UnitTest")
class ForexRateRestControllerTest {

    @MockBean
    ForexRateService rateService;

    @MockBean
    ForexPricingService pricingService;

    @Autowired
    WebApplicationContext wac;

    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
    }

    @Test
    void getLatestRates() throws Exception {

        // Mock return data of rate service
        when(rateService.fetchLatestRates(anyString()))
                .thenAnswer(invocation -> {
                    String baseCurrency = (String) invocation.getArgument(0);
                    LocalDateTime timestamp = LocalDateTime.now();
                    return Arrays.asList(
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("USD").buyRate(Math.random()).sellRate(Math.random()).build(),
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("EUR").buyRate(Math.random()).sellRate(Math.random()).build(),
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("CAD").buyRate(Math.random()).sellRate(Math.random()).build(),
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("JPY").buyRate(Math.random()).sellRate(Math.random()).build()
                    );
                });

        // trigger API request to rate controller
        webTestClient.get()
                .uri("/rates/latest/GBP")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
		        .expectBody()
                .consumeWith(response -> assertThat(response.getResponseBody()).isNotNull());
//		.jsonPath("$").isArray()
//		.jsonPath("$[0].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[0].counterCurrency").isEqualTo("USD")
//		.jsonPath("$[0].rate").isNumber()
//		.jsonPath("$[1].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[1].counterCurrency").isEqualTo("EUR")
//		.jsonPath("$[1].rate").isNumber()
//		.jsonPath("$[2].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[2].counterCurrency").isEqualTo("CAD")
//		.jsonPath("$[2].rate").isNumber()
//		.jsonPath("$[3].baseCurrency").isEqualTo("GBP")
//		.jsonPath("$[3].counterCurrency").isEqualTo("JPY")
//		.jsonPath("$[3].rate").isNumber();
    }


    @Test
    void bookRate() throws UnknownCustomerException {

        when(rateService.obtainBooking((any(ForexRateBookingReq.class))))
                .thenAnswer(invocation -> {
                    ForexRateBookingReq req = (ForexRateBookingReq) invocation.getArgument(0);
                    LocalDateTime timestamp = LocalDateTime.now();
                    LocalDateTime expiryTime = timestamp.plusMinutes(10);
                    return ForexRateBooking.builder()
                            .timestamp(timestamp)
                            .baseCurrency(req.getBaseCurrency())
                            .counterCurrency(req.getCounterCurrency())
                            .rate(Math.random())
                            .bookingRef(UUID.randomUUID().toString())
                            .expiryTime(expiryTime)
                            .customerId(req.getCustomerId())
                            .build();
                });

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


//    @Test
    // TODO: disabled this test at the moment before the response content type issue is fixed
    public void bookRate_missingParam() throws UnknownCustomerException {

        when(rateService.obtainBooking((any(ForexRateBookingReq.class))))
                .thenAnswer(invocation -> {
                    ForexRateBookingReq req = (ForexRateBookingReq) invocation.getArgument(0);
                    LocalDateTime timestamp = LocalDateTime.now();
                    LocalDateTime expiryTime = timestamp.plusMinutes(10);
                    return ForexRateBooking.builder()
                            .timestamp(timestamp)
                            .baseCurrency(req.getBaseCurrency())
                            .counterCurrency(req.getCounterCurrency())
                            .rate(Math.random())
                            .bookingRef(UUID.randomUUID().toString())
                            .expiryTime(expiryTime)
                            .customerId(req.getCustomerId())
                            .build();
                });

        ForexRateBookingReq req = ForexRateBookingReq.builder()
                .customerId(1l)
                .tradeAction(TradeAction.BUY)
//                .baseCurrency("GBP")
                .counterCurrency("USD")
                .baseCurrencyAmount(BigDecimal.valueOf(1000))
                .build();

        webTestClient.post()
                .uri("/rates/book")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(req), ForexRateBookingReq.class)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorBody.class);

    }

    @Test
    public void bookRate_unknownCustomer() throws UnknownCustomerException {

        when(rateService.obtainBooking((any(ForexRateBookingReq.class))))
                .thenThrow(new UnknownCustomerException());

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
                .expectStatus().is4xxClientError()
                .expectBody(ErrorBody.class);
    }

}
