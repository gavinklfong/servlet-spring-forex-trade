package space.gavinklfong.forex.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import space.gavinklfong.forex.dto.TradeAction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="forex_trade_deal")
public class ForexTradeDeal {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique = true)
	private String dealRef;
	
	private LocalDateTime timestamp;
		
	private String baseCurrency;
	
	private String counterCurrency;
	
	private Double rate;

	private TradeAction tradeAction;

	private BigDecimal baseCurrencyAmount;
	
//	@ManyToOne
//	@JoinColumn(name="customer_id", referencedColumnName="id")
//	private Customer customer;

	private Long customerId;

}
