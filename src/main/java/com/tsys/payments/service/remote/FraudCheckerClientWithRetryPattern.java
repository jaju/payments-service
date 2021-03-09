package com.tsys.payments.service.remote;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.FraudStatus;
import com.tsys.payments.domain.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Retry Pattern builds on top of Timeout Pattern.
 */
@Service
//@Qualifier("fraud_checker_retry")
@ConditionalOnExpression("#{'${features.resiliency.latency_control.strategy}' == 'retry'}")

// Below is expression for making retry as default instead of using a DefaultFraudCheckerClient
//@ConditionalOnExpression("#{'${features.resiliency.latency_control.strategy}' == 'retry' or '${features.resiliency.latency_control.strategy}' == '' }")
public class FraudCheckerClientWithRetryPattern implements FraudCheckerClient {

  private static final Logger LOG = Logger.getLogger(FraudCheckerClientWithRetryPattern.class.getName());

  @Autowired
  private RestTemplate restTemplate;

  private final URI fraudCheckerServiceUri;

  public FraudCheckerClientWithRetryPattern(@Value("${fraud-checker.service.url}") String hostname,
                                            @Value("${fraud-checker.service.port}") int port) {
    fraudCheckerServiceUri = URI.create(String.format("%s:%d/%s", hostname, port, "check"));
  }

  // SIMPLE RETRYABLE
//  @Retryable(value = {SocketTimeoutException.class},
//          maxAttempts = 2,
//         // maxAttempts = 4,
//          backoff = @Backoff(delay = 500)) // between 2 requests wait for 500 ms

  // EXPONENTIAL BACKOFF POLICY
  // ==========================
  // Spring has an exponential random backoff policy that will increase
  // the wait time for each retry attempt until a max retry time is reached
  // or the max number of attempts is reached.
  // It uses the following logic to achieve this:
  //
  // wait time = delay * (1.0D + random.nextFloat() * (multiplier - 1.0D))
  //
  //  if (delay > maxDelay) {
  //    delay = maxDelay
  //  } else {
  //    delay = delay * multiplier
  //  }
  //
  //  The delay, maxDelay and multiplier variables are set from the retry
  //  configuration that you implement. The random.nextFloat() is taken
  //  from the java.util package and this returns a random value between
  //  0 and 1.
  //  Therefore, if you set delay to 2000ms, maxDelay to 30000ms and
  //  multiplier to a value of 1.5 then the retry time will look something
  //  like the following for 4 attempts:
  //  request#     back off
  //    1              2000
  //    2              3000
  //    3              4500
  //    4              6750
  //
  //  The exponential random backoff can be configured with either the
  //  retry annotation or the retry template.
  //
  //  To implement the retry configuration noted above with the
  //  retry annotation then you need to update the Backoff policy to
  //  turn random indicator on as well as set the delay, max delay and
  //  multiplier values as shown below:
  //
//  @Retryable(value = {SocketTimeoutException.class},
//          maxAttempts = 4,
//          backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2))

  // UNIFORM RANDOM BACKOFF POLICY
  // =============================
  // Spring also offers a UniformRandomBackOffPolicy that waits for a
  // random period of time before retrying.
  // It uses the following logic to achieve this:
  //
  //  if (delay > maxDelay) {
  //    wait time = delay
  //  } else {
  //    wait time = delay + random.nextInt() * (maxDelay - delay))
  //  }
  //
  //  Similar, to the exponential random backoff this can be configured
  //  with either the retry annotation or the retry template.
  //  To implement the retry configuration noted above with the retry
  //  annotation then you need to update the Backoff policy to turn
  //  random indicator (but just leave out the multiplier and it implements
  //  the uniform backoff policy) on as well as set the delay and max delay
  //  values as shown below:
  //
//    @Retryable(
//            value = {SocketTimeoutException.class},
//            maxAttempts = 3,
//            backoff = @Backoff(random = true, delay = 100, maxDelay = 500)
//    )

  // Alternatively, these values can come from application.properties file
  // and one can tune these.
  @Retryable(
          include = {SocketTimeoutException.class},
          maxAttemptsExpression = "#{${latency_control.retry.maxAttempts}}",
          backoff = @Backoff(random = true,
                  delayExpression = "#{${latency_control.retry.backoff.delay}}",
                  maxDelayExpression = "#{${latency_control.retry.backoff.maxDelay}}")
  )

  // Using the above policy, you can maximise the number of concurrent
  // transactions that could hit a database at the same time.
  // You can get DB deadlock issues due to the nature of the
  // transaction but you can retry transactions that failed due
  // to DB deadlocks after a short wait using Spring retry logic
  // for problematic transaction. In particular, the uniform random
  // backoff policy allows to have a random wait interval between
  // a set range to reduce DB collisions.
  /**
   *  NOTE: Comment out the below @Recover fallback method when you just
   *  want to play with @Retryable without recovery.
   *  @see FraudCheckerClientWithRetryPattern#checkFraudFallback(CreditCard, Money)
   */
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
  @Recover
  public FraudStatus checkFraudFallback(CreditCard creditCard, Money chargedAmount) {
    LOG.info(() -> "FAILED Response <== FraudChecker MicroService");
    FraudStatus fraudStatus = allowChargingForSmallAmounts(creditCard, chargedAmount);

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
