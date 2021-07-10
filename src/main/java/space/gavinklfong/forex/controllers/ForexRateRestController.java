package space.gavinklfong.forex.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import space.gavinklfong.forex.dto.ForexRate;
import space.gavinklfong.forex.dto.ForexRateBookingReq;
import space.gavinklfong.forex.exceptions.InvalidRequestException;
import space.gavinklfong.forex.models.ForexRateBooking;
import space.gavinklfong.forex.services.ForexPricingService;
import space.gavinklfong.forex.services.ForexRateService;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/rates")
public class ForexRateRestController {

	@Value("${app.default-base-currency}")
	private String defaultBaseCurrency;

	@Autowired
	private ForexRateService rateService;

	@Autowired
	private ForexPricingService pricingService;

	@GetMapping(path = {"base-currencies", "base-currencies/"})
	public List<String> getBaseCurrencies() {

		return pricingService.findAllBaseCurrencies();

	}

	@GetMapping(path = {"latest", "latest/", "latest/{optionalBaseCurrency}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ForexRate> getLatestRates(@PathVariable Optional<String> optionalBaseCurrency) throws InvalidRequestException {

		String baseCurrency = optionalBaseCurrency.orElse(defaultBaseCurrency);

		return rateService.fetchLatestRates(baseCurrency);

	}

	@GetMapping(path = "latest/{baseCurrency}/{counterCurrency}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ForexRate getLatestRates(@PathVariable String baseCurrency, @PathVariable String counterCurrency) throws InvalidRequestException {

		return rateService.fetchLatestRate(baseCurrency, counterCurrency);

	}

	@PostMapping(path = "book", produces = MediaType.APPLICATION_JSON_VALUE)
	public ForexRateBooking bookRate(@Valid @RequestBody ForexRateBookingReq req) throws InvalidRequestException {

		// obtain booking
		return rateService.obtainBooking(req);

	}

}
