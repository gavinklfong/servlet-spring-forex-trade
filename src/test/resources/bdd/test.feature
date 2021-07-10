
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

	Scenario: Make a rate booking
		Given API Service is started
		When I request for a rate booking with parameters: '<baseCurrency>', '<counterCurrency>', '<tradeAction>', <baseCurrencyAmount>, <customerId>
		Then I should receive a valid rate booking
		Examples:
		| baseCurrency| counterCurrency | tradeAction | baseCurrencyAmount| customerId |
		| GBP 				| CAD 		| BUY		    | 1000							| 1 				 |

	Scenario: Submit a forex trade deal
		Given API Service is started
		And I request for a rate booking with parameters: '<baseCurrency>', '<counterCurrency>', '<tradeAction>', <baseCurrencyAmount>, <customerId>
		And I should receive a valid rate booking
		When I submit a forex trade deal with rate booking and parameters: '<baseCurrency>', '<counterCurrency>', '<tradeAction>', <baseCurrencyAmount>, <customerId>
		Then I should get the forex trade deal successfully posted
		Examples:
		| baseCurrency| counterCurrency | tradeAction | baseCurrencyAmount| customerId |
		| GBP 				| CAD 		| BUY		    | 1000			    | 1 				 |

	Scenario: Retrieve trade deal by customer
		Given API Service is started
		And I request for a rate booking with parameters: '<baseCurrency>', '<counterCurrency>', '<tradeAction>', <baseCurrencyAmount>, <customerId>
		And I should receive a valid rate booking
		When I submit a forex trade deal with rate booking and parameters: '<baseCurrency>', '<counterCurrency>', '<tradeAction>', <baseCurrencyAmount>, <customerId>
		And I should get the forex trade deal successfully posted
		When I request for forex trade deal by <customerId>
		Then I should get a list of forex trade deal for <customerId>
		Examples:
		| baseCurrency| counterCurrency | tradeAction | baseCurrencyAmount| customerId |
		| GBP 				| CAD 		| BUY		    | 1000			    | 1 				 |
