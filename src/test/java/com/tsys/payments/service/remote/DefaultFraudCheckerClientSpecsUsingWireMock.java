package com.tsys.payments.service.remote;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.FraudStatus;
import com.tsys.payments.domain.Money;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Currency;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

// What is WireMock?
// =================
// WireMock is a simulator for HTTP-based APIs, a library for stubbing and
// mocking web services. Use it when your service talks to one or many external or internal
// dependencies/services for which you might or might not have a test environment. Some
// might consider it a service virtualization tool or a mock server.
//
// A core feature of WireMock is the ability to return canned HTTP responses
// for requests matching criteria.  A lot of times, there are costs involved in hitting an
// external API. As integration tests are usually run during every regression (and most of
// the time with every commit), it might not be a cost-effective solution to hit such an
// API that costs us even for testing purposes.  It can be also used when an external API
// can not be configured to return the desired response.
// 1. It enables you to stay productive when an API you depend on doesn’t
//    exist or isn’t complete.
// 2. It supports the testing of edge cases and failure modes that the
//    real API won’t reliably produce.
// 3. Because it’s fast it can reduce your build time from hours down
//    to minutes.
//
// How does it work?
// =================
// It constructs a HTTP server that we could connect to as we would to
// an actual web service. It can be run with JUnit tests or can be made to run as a
// standalone server process.  It is configured via the Java API, JSON over HTTP or JSON
// files.
//
// PS: Read the $PROJECT_HOME/lib/test/WireMock-ReadMe.md before going further on this
// to get a good grounding on WireMock.
//
// We will now do all what you read programatically using WireMock and JUnit.
// At the same time we will add more cases to Ping.
//
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "fraud-checker.service.url = http://localhost",
        "fraud-checker.service.port = 8080",
        "features.resiliency.latency_control.strategy = "
})
// NOTE:  Ideally I would like to take these properties and use them to set
// the WireMock Server, however, I cannot use the @Value Spring annotation here
// on a static field (OPTIONS in our case) Spring supports this injection only
// on instance fields.
@Tag("IntegrationTest")
public class DefaultFraudCheckerClientSpecsUsingWireMock {

  @Value("${fraud-checker.service.url}")
  private String fraudCheckerServiceUrl;

  @Value("${fraud-checker.service.port}")
  private int fraudCheckerServicePort;

  private static final WireMockConfiguration OPTIONS = new WireMockConfiguration()
          .port(8080);
//          .withRootDirectory("src/test/resources/wiremock");

  private static final WireMockServer FRAUD_CHECKER_WEB_SERVICE = new WireMockServer(OPTIONS);

  @Autowired
  private DefaultFraudCheckerClient fraudCheckerClient;

  private final Money chargedAmount = new Money(Currency.getInstance("INR"), 1235.45d);

  private final CreditCard validCard = new CreditCard("4485-2847-2013-4093", "Jumping Jack", "Bank of Test", new Date(), 456);

  @BeforeAll
  public static void startFraudCheckerServer() {
    FRAUD_CHECKER_WEB_SERVICE.start();
  }

  @AfterEach
  public void resetFraudCheckerServer() {
    // WireMock server can be reset at any time, removing all stub mappings and
    // deleting the request log
    // or sending a POST request with an empty body to http://<host>:<port>/__admin/reset.
    FRAUD_CHECKER_WEB_SERVICE.resetAll();
  }

  @AfterAll
  public static void stopFraudCheckerServer() {
    FRAUD_CHECKER_WEB_SERVICE.stop();
  }

  @Test
  public void pingsFraudCheckerService() {
    assertNotNull(fraudCheckerClient);
    final String responseBody = "{ \"pong\" : \"I'm Alive!\"}";
    givenThat(get(urlEqualTo("/ping"))
            .willReturn(aResponse()
                    .withHeader("Content-Type", "text/plain")
                    .withStatus(200)
                    .withBody(responseBody)));

    // When-Then
    assertThat(fraudCheckerClient.ping(), is(responseBody));
  }

  @Test
  public void checksForFraudulentCreditCardTransactionAndMarksItPass() {
    givenThat(post(urlEqualTo("/check"))
            .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody("{\n" +
                            "    \"cvvStatus\": \"pass\",\n" +
                            "    \"avStatus\": \"pass\",\n" +
                            "    \"overall\": \"pass\"\n" +
                            "}")));

    // When
    final FraudStatus status = fraudCheckerClient.checkFraud(validCard, chargedAmount);

    // Then
    assertThat(status.overall, is("pass"));
  }

  @Test
  public void checksForFraudulentCreditCardTransactionAndMarksItFail() {
    givenThat(post(urlEqualTo("/check"))
            .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\n" +
                            "    \"cvvStatus\": \"fail\",\n" +
                            "    \"avStatus\": \"pass\",\n" +
                            "    \"overall\": \"fail\"\n" +
                            "}")));

    // When
    final FraudStatus status = fraudCheckerClient.checkFraud(validCard, chargedAmount);

    // Then
    assertThat(status.overall, is("fail"));
  }

  @Test
  public void checksForFraudulentCreditCardTransactionAndMarksItSuspicious() {
    givenThat(post(urlEqualTo("/check"))
            .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody("{\n" +
                            "    \"cvvStatus\": \"pass\",\n" +
                            "    \"avStatus\": \"incorrect address\",\n" +
                            "    \"overall\": \"suspicious\"\n" +
                            "}")));

    // When
    final FraudStatus status = fraudCheckerClient.checkFraud(validCard, chargedAmount);

    // Then
    assertThat(status.overall, is("suspicious"));
  }

  @Test
  public void shoutsWhenFraudCheckerServiceFails() {
    givenThat(post(urlEqualTo("/check"))
            .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{ \"error\" : \"Internal Server Error\" }")));

    // When
    assertThrows(HttpServerErrorException.class, () ->
            fraudCheckerClient.checkFraud(validCard, chargedAmount));
  }

  @Test
  public void shoutsWhenFraudCheckerServiceIsUnreachable() {
    final RestTemplate restTemplate = mock(RestTemplate.class);
    final URI fraudCheckUri = URI.create(fraudCheckerServiceUrl + "/ping");

    given(restTemplate.getForObject(fraudCheckUri, String.class))
            .willThrow(new RestClientException("Unreachable!"));

    final DefaultFraudCheckerClient fraudChecker = new DefaultFraudCheckerClient(fraudCheckerServiceUrl, fraudCheckerServicePort, restTemplate) {
      @Override
      URI createFraudCheckUri(String path) {
        return fraudCheckUri;
      }
    };

    // When-Then
    assertThrows(RestClientException.class, () -> fraudChecker.ping());
  }
}