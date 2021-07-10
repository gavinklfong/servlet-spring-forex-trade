package space.gavinklfong.forex.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import space.gavinklfong.forex.apiclients.ForexRateApiClient;
import space.gavinklfong.forex.dto.ForexPricing;
import space.gavinklfong.forex.dto.ForexRate;
import space.gavinklfong.forex.dto.ForexRateBookingReq;
import space.gavinklfong.forex.dto.TradeAction;
import space.gavinklfong.forex.exceptions.InvalidRequestException;
import space.gavinklfong.forex.exceptions.UnknownCustomerException;
import space.gavinklfong.forex.models.Customer;
import space.gavinklfong.forex.models.ForexRateBooking;
import space.gavinklfong.forex.models.ForexRateBooking.ForexRateBookingBuilder;
import space.gavinklfong.forex.repos.CustomerRepo;
import space.gavinklfong.forex.repos.ForexRateBookingRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForexRateService {

    private static Logger logger = LoggerFactory.getLogger(ForexRateService.class);

    @Value("${app.rate-booking-duration}")
    private long bookingDuration = 120l;

    @Autowired
    private ForexRateApiClient forexRateApiClient;

    @Autowired
    private ForexRateBookingRepo rateBookingRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private ForexPricingService forexPriceService;

    /**
     * Retrieve the latest rates for list of counter currencies
     * <p>
     * 1. Fetch a list of counter currency by the base currency
     * 2. Verify if base currency is available for price retrieval
     * 3. Consume Forex rate API to get the latest rate for the base currency
     * 4. Filter the records by list of country currency available for price retrieval
     * 5. Obtain Forex price for each currency pair
     * <p>
     * Flow:
     * Mono<ApiResponse> 					[Retrieve from Forex Rate API]
     * --> Flux<Map.Entry<String, Double>>  [Extract rates from response]
     * --> Flux<ForexRate> 					[Update rate based on price service]
     *
     * @param baseCurrency
     * @return Flux - Rate
     * @throws InvalidRequestException
     */
    public List<ForexRate> fetchLatestRates(String baseCurrency) {

        List<ForexPricing> counterCurrencyPricingList = forexPriceService.findCounterCurrencies(baseCurrency);

        if (counterCurrencyPricingList.isEmpty())
            throw new InvalidRequestException("base currency", "Invalid base currency");

        return forexRateApiClient.fetchLatestRates(baseCurrency)
                .getRates().entrySet().stream()
                .filter(rateEntry -> counterCurrencyPricingList.stream().anyMatch(p -> p.getCounterCurrency().equalsIgnoreCase(rateEntry.getKey())))
                .peek(rate -> log.debug(rate.toString()))
                .map(rate -> {
                    Double value = rate.getValue();
                    String key = rate.getKey();
                    return forexPriceService.obtainForexPrice(baseCurrency, key, value);
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the latest forex rate for the given base currency and counter currency
     *
     * @param baseCurrency
     * @param counterCurrency
     * @return Mono<ForexRate>
     * @throws InvalidRequestException
     */
    public ForexRate fetchLatestRate(String baseCurrency, String counterCurrency) {

        List<ForexPricing> counterCurrencyPricingList = forexPriceService.findCounterCurrencies(baseCurrency);

        if (counterCurrencyPricingList.isEmpty())
            throw new InvalidRequestException("base currency", "Invalid base currency");

        if (counterCurrencyPricingList.stream().noneMatch(c -> c.getCounterCurrency().equalsIgnoreCase(counterCurrency)))
            throw new InvalidRequestException("base currency", "Invalid counter currency");

        Double rate = forexRateApiClient.fetchLatestRate(baseCurrency, counterCurrency)
                .getRates().get(counterCurrency);

        return forexPriceService.obtainForexPrice(baseCurrency, counterCurrency, rate);
    }

    /**
     * Generate rate booking based on the latest rate and customer tier
     * <p>
     * <p>
     * Flow:
     * Mono<ApiResponse>					[Retrieve rate from forex API]
     * --> Mono<ForexRate>					[Construct ForexRate]
     * --> Mono<ForexRate>  			 	[Update rate by price service]
     * --> Tuple[<ForexRate>, <Customer>] 	[Retrieve customer record and zip together]
     * --> Mono<Forex>						[Adjust rate based on customer tier]
     * --> Mono<ForexRateBooking>			[Build rate booking object]
     * --> Mono<ForexRateBooking>			[Saved rate booking object with record id]
     *
     * @param request
     * @return Mono - RateBooking
     * @throws InvalidRequestException
     */
    public ForexRateBooking obtainBooking(ForexRateBookingReq request) {

        List<ForexPricing> forexPricingList = forexPriceService.findCounterCurrencies(request.getBaseCurrency());
        if (forexPricingList.stream().noneMatch(p -> p.getCounterCurrency().equalsIgnoreCase(request.getCounterCurrency()))) {
            throw new InvalidRequestException("currency", "unknown currency pair");
        }

        Double rate = forexRateApiClient
                .fetchLatestRate(request.getBaseCurrency(), request.getCounterCurrency()).getRates().get(request.getCounterCurrency());

        ForexRate rateWithPrice = forexPriceService.obtainForexPrice(
                        request.getBaseCurrency(),
                        request.getCounterCurrency(),
                        rate);

        Optional<Customer> customer = customerRepo.findById(request.getCustomerId());

        if (customer.isPresent()) {
            ForexRateBooking rateBooking = buildRateBookingRecord(request, adjustRateForCustomerTier(rateWithPrice, customer.get()));
            return rateBookingRepo.save(rateBooking);
        } else {
            throw new UnknownCustomerException();
        }

    }

    private ForexRate adjustRateForCustomerTier(ForexRate rate, Customer customer) {

        double adjustment = 0;

        // determine rate
        switch (customer.getTier()) {
            case 1:
                adjustment += CustomerRateTier.TIER1.rate;
                break;
            case 2:
                adjustment += CustomerRateTier.TIER2.rate;
                break;
            case 3:
                adjustment += CustomerRateTier.TIER3.rate;
                break;
            default:
                adjustment += CustomerRateTier.TIER4.rate;
        }

        return rate.withBuyRate(rate.getBuyRate() + adjustment).withSellRate(rate.getSellRate() + adjustment);
    }

    /**
     * Validate whether Rate Booking by
     * 1. Check if booking reference exists in repository
     * 2. Check if the record is expired
     *
     * @param rateBooking
     * @return
     */
    public Boolean validateRateBooking(ForexRateBooking rateBooking) {

        // Check existence of booking reference
        List<ForexRateBooking> repoRecords = rateBookingRepo.findByBookingRef(rateBooking.getBookingRef());

        if (repoRecords == null || repoRecords.size() <= 0) {
            return false;
        }

        // Check if booking reference is expired
        ForexRateBooking record = repoRecords.get(0);
        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Check currency pair
        if (!record.getBaseCurrency().equalsIgnoreCase(rateBooking.getBaseCurrency())
                || !record.getCounterCurrency().equalsIgnoreCase(rateBooking.getCounterCurrency())) {
            return false;
        }

        // Check amount
        if (record.getBaseCurrencyAmount().compareTo(rateBooking.getBaseCurrencyAmount()) != 0) {
            return false;
        }

        return true;
    }

    private ForexRateBooking buildRateBookingRecord(ForexRateBookingReq request, ForexRate rate) {

        LocalDateTime timestamp = LocalDateTime.now();
        LocalDateTime expiryTime = timestamp.plusSeconds(bookingDuration);

        ForexRateBookingBuilder builder = ForexRateBooking.builder()
                .baseCurrency(request.getBaseCurrency())
                .counterCurrency(request.getCounterCurrency())
                .baseCurrencyAmount(request.getBaseCurrencyAmount())
                .customerId(request.getCustomerId())
                .tradeAction(request.getTradeAction())
                .bookingRef(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .expiryTime(timestamp.plusSeconds(bookingDuration));

        if (request.getTradeAction() == TradeAction.SELL)
            builder.rate(rate.getSellRate());
        else if (request.getTradeAction() == TradeAction.BUY)
            builder.rate(rate.getBuyRate());
        else
            throw new RuntimeException("Unknown trade action");

        return builder.build();
    }
}
