package space.gavinklfong.forex.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@CucumberContextConfiguration
public class StepDef  {
	
	private static Logger logger = LoggerFactory.getLogger(StepDef.class);
	
	private String apiServiceUrl = "http://localhost:" + System.getProperty("test.server.port");
	
	private HttpResponse<String> response;
	
	private String baseCurrency;
	
	private String counterCurrency;
	
	private JSONObject rateBooking;
	
	@Given("^API Service is started$")
	public void api_service_is_started() throws IOException {

		// ping if application is up and running
		int appPort = Integer.parseInt(System.getProperty("test.server.port"));
		
	    Socket socket = new Socket();
	    socket.connect(new InetSocketAddress("localhost", appPort), 1000);
	    socket.close();    
				
	}
	
	@When("I request for the latest rate with base currency {string}")
	public void i_request_for_the_latest_rate_with_base_currency(String base) throws URISyntaxException, IOException, InterruptedException {

		this.baseCurrency = base;
		
		// Send request to get the latest rates
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest
				.newBuilder(new URI(apiServiceUrl + "/rates/latest/" + base))
				.header("accept", "application/json").build();	
		this.response = client.send(request, HttpResponse.BodyHandlers.ofString());
		
	}
	
	@Then("I should receive list of currency rate")
	public void i_should_receive_list_of_currency_rate() {

		assertEquals(200, response.statusCode());		
		
		JSONArray jsonArray = new JSONArray(response.body());
		logger.debug("record size: " + jsonArray.length());

		assertTrue(jsonArray.length() > 2);
		
		jsonArray.forEach(item -> {
			JSONObject json = (JSONObject) item;
			assertTrue(json.getDouble("buyRate") > 0);
			assertTrue(json.getDouble("sellRate") > 0);
			assertEquals(baseCurrency, json.getString("baseCurrency"));
			assertNotEquals(baseCurrency, json.getString("counterCurrency"));
		});
	}
	
	@When("I request for a rate booking with parameters: {string}, {string}, {string}, {long}, {long}")
	public void i_request_for_a_rate_booking_with_parameters(String base, String counter, String tradeAction, Long amount, Long customerId) throws URISyntaxException, IOException, InterruptedException {
		
		this.baseCurrency = base;
		this.counterCurrency = counter;

		JSONObject rateBookingReq = new JSONObject();
		rateBookingReq.put("baseCurrency", base);
		rateBookingReq.put("counterCurrency", counter);
		rateBookingReq.put("tradeAction", tradeAction);
		rateBookingReq.put("baseCurrencyAmount", amount);
		rateBookingReq.put("customerId", customerId);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest
				.newBuilder(new URI(apiServiceUrl + "/rates/book"))
				.POST(HttpRequest.BodyPublishers.ofString(rateBookingReq.toString()))
				.header("accept", "application/json")
				.header("Content-Type", "application/json")
				.build();

		this.response = client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	@Then("I should receive a valid rate booking")
	public void i_should_receive_a_valid_rate_booking() {

		logger.debug(response.body());
		
		assertEquals(200, response.statusCode());
		
		JSONObject json = new JSONObject(response.body());

		assertTrue(json.getDouble("rate") > 0);
		assertEquals(baseCurrency, json.getString("baseCurrency"));
		assertEquals(counterCurrency, json.getString("counterCurrency"));
		assertNotNull(json.getString("bookingRef"));
		LocalDateTime expiryTime = LocalDateTime.parse(json.getString("expiryTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		assertTrue(expiryTime.isAfter(LocalDateTime.now()));
		
		this.rateBooking = json;
	}	
	
	@When("I submit a forex trade deal with rate booking and parameters: {string}, {string}, {string}, {long}, {long}")
	public void i_submit_a_forex_trade_deal_with_rate_booking_and_parameters(String base, String counter, String tradeAction, Long amount, Long customerId) throws URISyntaxException, IOException, InterruptedException {
		
		JSONObject tradeDeal = new JSONObject();
		tradeDeal.put("baseCurrency", base);
		tradeDeal.put("counterCurrency", counter);
		tradeDeal.put("tradeAction", tradeAction);
		tradeDeal.put("baseCurrencyAmount", amount);
		tradeDeal.put("customerId", customerId);
		tradeDeal.put("rateBookingRef", rateBooking.getString("bookingRef"));
		tradeDeal.put("rate", rateBooking.getDouble("rate"));
		
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest
				.newBuilder(new URI(apiServiceUrl + "/deals"))
				.POST(HttpRequest.BodyPublishers.ofString(tradeDeal.toString()))
				.header("accept", "application/json")
				.header("Content-Type", "application/json")
				.build();	
		this.response = client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	@Then("I should get the forex trade deal successfully posted")
	public void i_should_get_the_forex_trade_deal_successfully_posted() {
		
		assertEquals(200, response.statusCode());
		
		JSONObject json = new JSONObject(response.body());
		
		assertEquals(baseCurrency, json.getString("baseCurrency"));
		assertEquals(counterCurrency, json.getString("counterCurrency"));
		assertEquals(rateBooking.getDouble("rate"), json.getDouble("rate"));
		assertTrue(json.getString("dealRef").trim().length() > 0);
		assertTrue(json.getLong("id") > 0);		
	}
	
	@When("I request for forex trade deal by {long}")
	public void i_request_for_forex_trade_deal_by(Long customerId) throws URISyntaxException, IOException, InterruptedException {
		// Send request to get the latest rates
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest
				.newBuilder(new URI(apiServiceUrl + "/deals?customerId=" + customerId))
				.header("accept", "application/json").build();	
		this.response = client.send(request, HttpResponse.BodyHandlers.ofString());			
	}
	
	@Then("I should get a list of forex trade deal for {long}")
	public void i_should_get_a_list_of_forex_trade_deal_for(Long customer) {
		
		JSONArray jsonArray = new JSONArray(response.body());
		
		assertTrue(jsonArray.length() > 0);
		jsonArray.forEach(item -> {
			JSONObject json = (JSONObject) item;
			assertTrue(json.getString("dealRef").trim().length() > 0);
			assertNotNull(json.getString("baseCurrency"));
			assertNotNull(json.getString("counterCurrency"));
		});
		
	}

	
}
