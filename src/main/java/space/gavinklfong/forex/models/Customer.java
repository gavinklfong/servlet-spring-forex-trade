package space.gavinklfong.forex.models;

import lombok.Data;

import javax.persistence.*;
import java.util.Collection;

@Data
@Entity
@Table(name = "customer")
public class Customer {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
	private Integer tier;
	
//	@OneToMany (cascade = CascadeType.ALL, mappedBy = "customer", orphanRemoval = true)
//	private Collection<ForexTradeDeal> deals;
	
	public Customer() {
		super();
	}
	
	public Customer(Long id) {
		this.id = id;
	}
	
	public Customer(String name, Integer tier) {
		super();
		this.name = name;
		this.tier = tier;
	}

	public Customer(Long id, String name, Integer tier) {
		super();
		this.id = id;
		this.name = name;
		this.tier = tier;
	}

}
