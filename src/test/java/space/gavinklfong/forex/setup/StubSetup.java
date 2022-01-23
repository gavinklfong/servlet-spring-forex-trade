package space.gavinklfong.forex.setup;

import lombok.experimental.UtilityClass;
import space.gavinklfong.forex.dto.ForexRate;
import space.gavinklfong.forex.dto.ForexRateBookingReq;
import space.gavinklfong.forex.dto.ForexTradeDealReq;
import space.gavinklfong.forex.models.ForexRateBooking;
import space.gavinklfong.forex.models.ForexTradeDeal;
import space.gavinklfong.forex.services.ForexRateService;
import space.gavinklfong.forex.services.ForexTradeService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@UtilityClass
public class StubSetup {

    public void stubForGetForexRates(ForexRateService forexRateService) {
        when(forexRateService.fetchLatestRates(anyString()))
                .thenAnswer(invocation -> {
                    String baseCurrency = (String) invocation.getArgument(0);
                    Instant timestamp = Instant.now();
                    return Arrays.asList(
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("USD").buyRate(Math.random()).sellRate(Math.random()).build(),
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("EUR").buyRate(Math.random()).sellRate(Math.random()).build(),
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("CAD").buyRate(Math.random()).sellRate(Math.random()).build(),
                            ForexRate.builder().timestamp(timestamp).baseCurrency(baseCurrency).counterCurrency("JPY").buyRate(Math.random()).sellRate(Math.random()).build()
                    );
                });
    }

    public void stubForGetForexRate(ForexRateService forexRateService) {
        when(forexRateService.fetchLatestRate(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String baseCurrency = (String) invocation.getArgument(0);
                    String counterCurrency = (String) invocation.getArgument(1);
                    Instant timestamp = Instant.now();
                    return ForexRate.builder()
                            .timestamp(timestamp)
                            .baseCurrency(baseCurrency)
                            .counterCurrency(counterCurrency)
                            .buyRate(Math.random())
                            .sellRate(Math.random())
                            .build();
                });
    }

    public void stubForBookRate(ForexRateService forexRateService) {
        when(forexRateService.obtainBooking((any(ForexRateBookingReq.class))))
                .thenAnswer(invocation -> {
                    ForexRateBookingReq req = (ForexRateBookingReq) invocation.getArgument(0);
                    Instant timestamp = Instant.now();
                    Instant expiryTime = timestamp.plus(Duration.ofMinutes(10));
                    return ForexRateBooking.builder()
                            .id((long)Math.random())
                            .timestamp(timestamp)
                            .baseCurrency(req.getBaseCurrency())
                            .counterCurrency(req.getCounterCurrency())
                            .rate(Math.random())
                            .tradeAction(req.getTradeAction())
                            .baseCurrencyAmount(req.getBaseCurrencyAmount())
                            .bookingRef(UUID.randomUUID().toString())
                            .expiryTime(expiryTime)
                            .customerId(req.getCustomerId())
                            .build();
                });
    }

    public void stubForSubmitDeal(ForexTradeService tradeService) {
        when(tradeService.postTradeDeal(any(ForexTradeDealReq.class)))
                .thenAnswer(invocation -> {
                    ForexTradeDealReq req = (ForexTradeDealReq)invocation.getArgument(0);
                    Instant timestamp = Instant.now();
                    return 	ForexTradeDeal.builder()
                            .id(1l).dealRef(UUID.randomUUID().toString())
                            .timestamp(Instant.now())
                            .baseCurrency(req.getBaseCurrency()).counterCurrency(req.getCounterCurrency())
                            .rate(req.getRate()).baseCurrencyAmount(req.getBaseCurrencyAmount()).customerId(req.getCustomerId())
                            .tradeAction(req.getTradeAction())
                            .build();
                });
    }
}
