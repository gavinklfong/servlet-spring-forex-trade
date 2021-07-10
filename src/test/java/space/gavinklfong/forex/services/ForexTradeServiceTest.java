package space.gavinklfong.forex.services;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import space.gavinklfong.forex.dto.ForexTradeDealReq;
import space.gavinklfong.forex.dto.TradeAction;
import space.gavinklfong.forex.exceptions.InvalidRateBookingException;
import space.gavinklfong.forex.exceptions.UnknownCustomerException;
import space.gavinklfong.forex.models.Customer;
import space.gavinklfong.forex.models.ForexRateBooking;
import space.gavinklfong.forex.models.ForexTradeDeal;
import space.gavinklfong.forex.repos.CustomerRepo;
import space.gavinklfong.forex.repos.ForexTradeDealRepo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@ContextConfiguration(classes = {ForexTradeService.class})
@Tag("UnitTest")
public class ForexTradeServiceTest {

	@MockBean
	private CustomerRepo customerRepo;
	
	@MockBean
	private ForexTradeDealRepo tradeDealRepo;
	
	@MockBean
	private ForexRateService rateService;
	
	@Autowired
	private ForexTradeService tradeService;
	
	@Test
	public void postTradeDealTest_success() {
		
		when(tradeDealRepo.save(any(ForexTradeDeal.class))).thenAnswer(invocation -> {
			ForexTradeDeal deal = (ForexTradeDeal) invocation.getArgument(0);
			deal.setId(99l);
			return deal;
		});
		
		when(customerRepo.findById(anyLong()))
		.thenReturn(Optional.of(new Customer(1l, "Tester 1", 1)));
		
		when(rateService.validateRateBooking(any(ForexRateBooking.class))).thenReturn(true);

		ForexTradeDealReq req = ForexTradeDealReq.builder()
				.tradeAction(TradeAction.BUY)
				.baseCurrency("GBP")
				.counterCurrency("USD")
				.rate(0.25)
				.baseCurrencyAmount(BigDecimal.valueOf(10000))
				.customerId(1l)
				.rateBookingRef("ABC")
				.build();
		
		ForexTradeDeal deal = tradeService.postTradeDeal(req);
		assertEquals(99l, deal.getId());
	}
	
	@Test
	public void postTradeDealTest_invalidRateBooking() {
		
		when(tradeDealRepo.save(any(ForexTradeDeal.class))).thenAnswer(invocation -> {
			ForexTradeDeal deal = (ForexTradeDeal) invocation.getArgument(0);
			deal.setId(99l);
			return deal;
		});

		when(customerRepo.findById(anyLong()))
		.thenReturn(Optional.of(new Customer(1l, "Tester 1", 1)));

		when(rateService.validateRateBooking(any(ForexRateBooking.class))).thenReturn(false);

		ForexTradeDealReq req = ForexTradeDealReq.builder()
				.tradeAction(TradeAction.BUY)
				.baseCurrency("GBP")
				.counterCurrency("USD")
				.rate(0.25)
				.baseCurrencyAmount(BigDecimal.valueOf(10000))
				.customerId(1l)
				.rateBookingRef("ABC")
				.build();

		assertThrows(InvalidRateBookingException.class, () -> {
			ForexTradeDeal deal = tradeService.postTradeDeal(req);
		});

	}
	
	@Test
	public void postTradeDealTest_unknownCustomer() {
		
		when(tradeDealRepo.save(any(ForexTradeDeal.class))).thenAnswer(invocation -> {
			ForexTradeDeal deal = (ForexTradeDeal) invocation.getArgument(0);
			deal.setId(99l);
			return deal;
		});
		
		when(customerRepo.findById(anyLong()))
		.thenReturn(Optional.empty());
		
		when(rateService.validateRateBooking(any(ForexRateBooking.class))).thenReturn(true);

		ForexTradeDealReq req = ForexTradeDealReq.builder()
				.tradeAction(TradeAction.BUY)
				.baseCurrency("GBP")
				.counterCurrency("USD")
				.rate(0.25)
				.baseCurrencyAmount(BigDecimal.valueOf(10000))
				.customerId(1l)
				.rateBookingRef("ABC")
				.build();

		assertThrows(UnknownCustomerException.class, () -> {
			ForexTradeDeal deal = tradeService.postTradeDeal(req);
		});

	}
	
	@Test
	public void retrieveTradeDealByCustomerTest() {

		ForexTradeDeal deal1 = ForexTradeDeal.builder()
				.id(1l).dealRef(UUID.randomUUID().toString())
				.timestamp(LocalDateTime.now())
				.baseCurrency("GBP").counterCurrency("USD")
				.rate(1.25d).baseCurrencyAmount(new BigDecimal(1000)).customerId(1l)
				.tradeAction(TradeAction.BUY)
				.build();

		ForexTradeDeal deal2 = ForexTradeDeal.builder()
				.id(1l).dealRef(UUID.randomUUID().toString())
				.timestamp(LocalDateTime.now())
				.baseCurrency("GBP").counterCurrency("USD")
				.rate(1.25d).baseCurrencyAmount(new BigDecimal(1000)).customerId(1l)
				.tradeAction(TradeAction.SELL)
				.build();

		ForexTradeDeal deal3 = ForexTradeDeal.builder()
				.id(1l).dealRef(UUID.randomUUID().toString())
				.timestamp(LocalDateTime.now())
				.baseCurrency("GBP").counterCurrency("USD")
				.rate(Math.random()).baseCurrencyAmount(new BigDecimal(1000)).customerId(1l)
				.tradeAction(TradeAction.SELL)
				.build();
				
		List<ForexTradeDeal> deals = new ArrayList<>();
		deals.add(deal1);
		deals.add(deal2);
		deals.add(deal3);
		
		when(tradeDealRepo.findByCustomerId(anyLong())).thenReturn(deals);
		
		List<ForexTradeDeal> result = tradeService.retrieveTradeDealByCustomer(1l);
		assertEquals(3, result.size());
		assertTrue(deal1.equals(result.get(0)));
		assertTrue(deal2.equals(result.get(1)));
		assertTrue(deal3.equals(result.get(2)));

	}
	
	@Test
	public void retrieveTradeDealByCustomerTest_Empty() {
		
		when(tradeDealRepo.findByCustomerId(anyLong())).thenReturn(null);
		
		List<ForexTradeDeal> result = tradeService.retrieveTradeDealByCustomer(1l);

		assertNull(result);
	}
	
}
