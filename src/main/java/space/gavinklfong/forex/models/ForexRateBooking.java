package space.gavinklfong.forex.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import space.gavinklfong.forex.dto.TradeAction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "forex_rate_booking")
public class ForexRateBooking {

	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private Long id;
	
	private Instant timestamp;
	private String baseCurrency;
	private String counterCurrency;
	private Double rate;
	private TradeAction tradeAction;
	private BigDecimal baseCurrencyAmount;
	
	@Column(unique = true)
	private String bookingRef;
	
	private Instant expiryTime;

//	@ManyToOne
//	@JoinColumn(name = "customer_id", referencedColumnName = "id")
//	private Customer customer;

	private Long customerId;

	public ForexRateBooking(Long id, Instant timestamp, String baseCurrency, String counterCurrency, Double rate,
							String bookingRef, Instant expiryTime, Long customerId) {
		super();
		this.id = id;
		this.timestamp = timestamp;
		this.baseCurrency = baseCurrency;
		this.counterCurrency = counterCurrency;
		this.rate = rate;
		this.bookingRef = bookingRef;
		this.expiryTime = expiryTime;
		this.customerId = customerId;
	}

	public ForexRateBooking(String baseCurrency, String counterCurrency, Double rate, BigDecimal baseCurrencyAmount, String bookingRef) {
		super();
		this.baseCurrency = baseCurrency;
		this.counterCurrency = counterCurrency;
		this.rate = rate;
		this.baseCurrencyAmount = baseCurrencyAmount;
		this.bookingRef = bookingRef;
	}

	public ForexRateBooking(String baseCurrency, String counterCurrency, BigDecimal baseCurrencyAmount, Long customerId) {
		super();
		this.baseCurrency = baseCurrency;
		this.counterCurrency = counterCurrency;
		this.baseCurrencyAmount = baseCurrencyAmount;
		this.customerId = customerId;
	}

}
