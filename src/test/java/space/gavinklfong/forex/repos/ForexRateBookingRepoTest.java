package space.gavinklfong.forex.repos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import space.gavinklfong.forex.models.Customer;
import space.gavinklfong.forex.models.ForexRateBooking;


@Slf4j
//@SpringJUnitConfig
//@ContextConfiguration(classes = {R2DBCConfig.class})
//@DataR2dbcTest
@Tag("UnitTest")
@SpringBootTest
public class ForexRateBookingRepoTest {

	@Autowired
	DatabaseClient database;		
    
	@Autowired
	private ForexRateBookingRepo rateBookingRepo;
	
	
	@Before
	public void setUp() {
		Hooks.onOperatorDebug();
		
		List<String> customerSql = Arrays.asList(
				"drop table if exists customer CASCADE;",
				"create table customer (id bigint generated by default as identity, name varchar(255), tier integer, primary key (id));",
				"INSERT INTO customer (name, tier) VALUES ('tester 1', 1);",
				"INSERT INTO customer (name, tier) VALUES ('tester 2', 1);",
				"INSERT INTO customer (name, tier) VALUES ('tester 3', 1);"
				);

		List<String> forexRateBookingSql = Arrays.asList(
				"drop table if exists forex_rate_booking CASCADE;",
				"create table forex_rate_booking (id bigint generated by default as identity, base_currency varchar(255), base_currency_amount decimal(19,2), booking_ref varchar(255), counter_currency varchar(255), expiry_time timestamp, rate double, timestamp timestamp, customer_id bigint, primary key (id));",				
				"INSERT INTO forex_rate_booking (timestamp, base_currency, counter_currency, rate, base_currency_amount, booking_ref, expiry_time, customer_id) "
				+ "VALUES ('2021-02-01 11:50:00', 'GBP', 'USD', 1.3690754045, 1000, 'BOOKING-REF-01', '2021-02-01 12:10:00', 1);"
				);
		
		
		List<String> forexTradeDealSql = Arrays.asList(
				"drop table if exists forex_trade_deal CASCADE;",
				"create table forex_trade_deal (id bigint generated by default as identity, base_currency varchar(255), base_currency_amount decimal(19,2), counter_currency varchar(255), deal_ref varchar(255), rate double, timestamp timestamp, customer_id bigint, primary key (id));",
				"INSERT INTO forex_trade_deal (timestamp, deal_ref, base_currency, counter_currency, rate, base_currency_amount, customer_id) "
				+ "VALUES ('2021-02-01 12:00:00', 'DEAL-REF-01', 'GBP', 'USD', 1.3690754045, 1000, 1);",
				"INSERT INTO forex_trade_deal (timestamp, deal_ref, base_currency, counter_currency, rate, base_currency_amount, customer_id) "
				+ "VALUES ('2021-02-02 12:00:00', 'DEAL-REF-02', 'EUR', 'CAD', 1.5331, 2000, 1);",
				"INSERT INTO forex_trade_deal (timestamp, deal_ref, base_currency, counter_currency, rate, base_currency_amount, customer_id) "
				+ "VALUES ('2021-02-03 12:00:00', 'DEAL-REF-03', 'USD', 'EUR', 0.8250144378, 3000, 1);"				
				);
		
		customerSql.forEach(it -> database.sql(it) 
				.fetch() 
				.rowsUpdated() 
				.as(StepVerifier::create) 
				.expectNextCount(3) 
				.verifyComplete());		

		forexRateBookingSql.forEach(it -> database.sql(it) 
				.fetch() 
				.rowsUpdated() 
				.as(StepVerifier::create) 
				.expectNextCount(1) 
				.verifyComplete());			

		forexTradeDealSql.forEach(it -> database.sql(it) 
				.fetch() 
				.rowsUpdated() 
				.as(StepVerifier::create) 
				.expectNextCount(3) 
				.verifyComplete());	
	}
	
	@DisplayName("save rate booking")
	@Test
	void testSave() {
		
		UUID uuid = UUID.randomUUID();
		
		ForexRateBooking rate = new ForexRateBooking();
		rate.setBaseCurrency("GBP");
		rate.setCounterCurrency("USD");
		rate.setTimestamp(LocalDateTime.now());
		rate.setRate(Double.valueOf(2.25));
		rate.setExpiryTime(LocalDateTime.now().plusMinutes(10));
		rate.setBookingRef(uuid.toString());
		
		Customer cust = new Customer();
		cust.setName("tester");
		cust.setTier(1);
		
		rate.setCustomer(cust);
		
		
		Mono<ForexRateBooking> savedRate = rateBookingRepo.save(rate);
		
		assertNotNull(savedRate.block().getId());
		
		
	}
	
	@DisplayName("find all rate booking")
	@Test
	void testFindAll() {
		
		UUID uuid = UUID.randomUUID();
		
		
		ForexRateBooking bookingOriginal = new ForexRateBooking();
		bookingOriginal.setBaseCurrency("GBP");
		bookingOriginal.setCounterCurrency("USD");
		bookingOriginal.setTimestamp(LocalDateTime.now());
		bookingOriginal.setRate(Double.valueOf(2.25));
		bookingOriginal.setExpiryTime(LocalDateTime.now().plusMinutes(10));
		bookingOriginal.setBookingRef(uuid.toString());
		
		Mono<ForexRateBooking> savedBooking = rateBookingRepo.save(bookingOriginal);
		savedBooking.block();
		
		Flux<ForexRateBooking> bookings = rateBookingRepo.findAll();
		

		int count = bookings.collectList()
		.map(c -> c.size()).block();
		
		assertTrue(count > 0);
	}	
	
	@DisplayName("find by booking ref")
	@Test
	void findByBookingRef_withRecord() {
		
		ForexRateBooking booking = new ForexRateBooking();
		booking.setTimestamp(LocalDateTime.parse("2021-02-01T11:50:00"));
		booking.setExpiryTime(LocalDateTime.parse("2021-02-01T12:10:00"));
		booking.setBaseCurrency("GBP");
		booking.setCounterCurrency("USD");
		booking.setBaseCurrencyAmount(new BigDecimal(1000));
		booking.setBookingRef("BOOKING-REF-01");
		booking.setRate(1.3690754045);
		
		rateBookingRepo.findByBookingRef("BOOKING-REF-01")
		.as(StepVerifier::create)
		.expectNextMatches(item -> item.getTimestamp().equals(booking.getTimestamp()))
		.expectComplete()
		.verify();
		
//		assertNotNull(bookings);
//		assertEquals(1, bookings.size());
//		
//		ForexRateBooking booking = bookings.get(0);
//		assertEquals("2021-02-01T11:50:00", booking.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
//		assertEquals("2021-02-01T12:10:00", booking.getExpiryTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
//		assertEquals("GBP", booking.getBaseCurrency());
//		assertEquals("USD", booking.getCounterCurrency());
//		assertEquals(1000d, booking.getBaseCurrencyAmount().doubleValue());	
	}
	
	@DisplayName("find by booking ref - no record")
	@Test
	void findByBookingRef_noRecord() {

		rateBookingRepo.findByBookingRef("BOOKING-REF-02")
		.as(StepVerifier::create)		
		.verifyComplete();
		

	}
	
}
