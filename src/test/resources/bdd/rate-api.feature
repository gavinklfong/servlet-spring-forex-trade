
Feature: Rate Service
	Verify Rate Service Functionalities
	
	Scenario: Fetch the latest rate with base currency '<baseCurrency>'
		Given API Service is started
		When I request for the latest rate with base currency '<baseCurrency>'
		Then I should receive list of currency rate
		Examples:
		| baseCurrency |
		| GBP |
		| USD |
		| AUD |
		| EUR |
		| NZD |

	Scenario: Make a rate booking
		Given API Service is started
		When I request for a rate booking with parameters: '<baseCurrency>', '<counterCurrency>', '<tradeAction>', <baseCurrencyAmount>, <customerId>
		Then I should receive a valid rate booking
		Examples:
		| baseCurrency| counterCurrency | tradeAction | baseCurrencyAmount| customerId |
		| EUR 				| USD 		| BUY		    | 1000							| 1 				 |
		| USD 				| JPY 		| BUY			| 250							| 1 				 |
		| GBP 				| USD 		| SELL			| 2000							| 1 				 |
		| AUD 				| USD 		| SELL			| 3000							| 1 				 |
		| NZD 				| USD 		| BUY			| 1500							| 1 				 |
		| EUR 				| JPY 		| SELL			| 100000						| 1 				 |
		