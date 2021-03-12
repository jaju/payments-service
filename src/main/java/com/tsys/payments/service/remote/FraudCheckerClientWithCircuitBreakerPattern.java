package com.tsys.payments.service.remote;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.FraudStatus;
import com.tsys.payments.domain.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Circuit Breaker builds on top of Retry Pattern and Timeout Pattern.
 */
@Service
//@Qualifier("fraud_checker_cb")
@ConditionalOnExpression("#{'${features.resiliency.latency_control.strategy}' == 'circuit_breaker'}")
public class FraudCheckerClientWithCircuitBreakerPattern implements FraudCheckerClient {

    private static final Logger LOG = Logger.getLogger(FraudCheckerClientWithCircuitBreakerPattern.class.getName());
    private final URI fraudCheckerServiceUri;
    @Autowired
    private RestTemplate restTemplate;

    public FraudCheckerClientWithCircuitBreakerPattern(@Value("${fraud-checker.service.host}") String hostname,
                                                       @Value("${fraud-checker.service.port}") int port) {
        fraudCheckerServiceUri = URI.create(String.format("%s:%d/%s", hostname, port, "check"));
    }

    //  Using Circuit Breaker with Spring Retry
    //  =======================================
    //  1. To enable the Spring Retry - Annotate the Application or
    //     Configuration class with @EnableRetry.
    //  2. The annotation for CircuitBreaker is: @CircuitBreaker.  It
    //     encapsulates the @Retryable(stateful = true), that means the same
    //     request will return the same response.
    //
    //  Attributes for the @CircuitBreaker are:
    //  1. maxAttempts - Max attempts before starting calling the @Recover method
    //                   annotated.
    //  2. openTimeout - If the maxAttempts fails inside this timeout, the
    //                   recover method starts to get called.
    //  3. resetTimeout - If the circuit is open after this timeout, the next
    //                    call will give the chance for system to return.
    //
    // For example, if maxAttempts is reached within the openTimeout,
    // then the circuit is open and the next request goes to the @Recover
    // method. However, after the resetTimeout, the circuit closes and the
    // method is called again.
    //
    // This is very useful when calling 3rd party services esp. when it fails
    // to service requests. The time of resetTimeout is the time that
    // the system has for recovery (from too many request, IO lock etc...).
    @CircuitBreaker(include = {SocketTimeoutException.class},
            maxAttempts = 2,
            openTimeout = 15000, // millis
            resetTimeout = 30000) // millis
    public FraudStatus checkFraud(CreditCard creditCard, Money chargedAmount) {
        Map<String, Object> request = new HashMap<>() {{
            put("creditCard", creditCard);
            put("charge", chargedAmount);
        }};
        LOG.info(() -> String.format("Sending Request ==> FraudChecker MicroService on %s", fraudCheckerServiceUri));
        FraudStatus fraudStatus = restTemplate.postForObject(fraudCheckerServiceUri, request, FraudStatus.class);
        LOG.info(() -> String.format("Got Response <== FraudChecker MicroService %s", fraudStatus));
        return fraudStatus;
    }

    //  FALLBACK
    //  ========
    //  We send the request to the FraudChecker service and it may be
    //  retried couple of times when the response is not obtained.
    //  But still after exhausting all the tries, the response may not
    //  be obtained and this will result in a failure to Payment Services
    //  Clients.  To recover from the failure, we create another method
    //  with @Recover. The @Recover annotation defines a separate recovery
    //  method when a @Retryable method fails with a specified exception.
    //  It is responsible for telling the application what needs to be done
    //  when FraudChecker Service does not respond with in time. In this case,
    //  we create an empty response, so that any intermittent network related
    //  issues do not affect the core services behavior.
    //
    //  Fallback values are not always possible but can greatly increase
    //  overall resilience if used carefully. In this example it can be
    //  dangerous to fallback to treating the transaction as not fraudulent
    //  in case the fraud check service is not available. It may open up
    //  an attack surface for fraudulent transactions attempting to
    //  first spam the service and then place the fraudulent transaction.
    //
    //  On the other hand, if the fallback is to assume that every
    //  transaction is fraudulent (like what is done below, no payment will
    //  go through and the fallback is essentially useless.
    //
//  @Recover
//  public FraudStatus checkFraudFallback() {
//    LOG.info(() -> "FAILED Response <== FraudChecker MicroService");
//    FraudStatus empty = FraudStatus.EMPTY;
//    LOG.info(() -> String.format("RECOVERY from FAILED response with %s", empty));
//    return empty;
//  }

    //  A good compromise might be to fallback to a simple business rule,
    //  e.g. simply letting transactions with a reasonably small amount
    //  through to have a good balance between risk and not losing customers.
    //
    //  The question then is - How to pass original request arguments to the
    //  recovery method?
    //  Answer: The arguments for the recovery method can optionally include
    //  the exception that was thrown, and also optionally the arguments
    //  passed to the original Retryable method (or a partial list of them
    //  as long as none are omitted).  Below, we are not using Exception as
    //  the first parameter, instead using the same arguments as in checkFraud()
    //  method.
    //
    @Override
    @Recover
    public FraudStatus checkFraudFallback(CreditCard creditCard, Money chargedAmount) {
        LOG.info(() -> "FAILED Response <== FraudChecker MicroService");
        var fraudStatus = allowChargingForSmallAmounts(creditCard, chargedAmount);

        LOG.info(() -> String.format("RECOVERY from FAILED response with %s", fraudStatus));
        return fraudStatus;
    }

    private FraudStatus allowChargingForSmallAmounts(CreditCard creditCard, Money chargedAmount) {
        Money allowableAmount = new Money(chargedAmount.currency, 1000d);
        if (chargedAmount.lessThan(allowableAmount)) {
            LOG.info(() -> String.format("Allowing Small Amount %s to be Charged to Credit Card %s", chargedAmount, creditCard));
            return new FraudStatus("unverified");
        }
        return FraudStatus.EMPTY;
    }
}
