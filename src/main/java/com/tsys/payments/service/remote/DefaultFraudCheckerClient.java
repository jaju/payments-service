package com.tsys.payments.service.remote;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.FraudStatus;
import com.tsys.payments.domain.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
//@Qualifier("fraud_checker_retry")
@ConditionalOnExpression("#{'${features.resiliency.latency_control.strategy}' == '' }")
public class DefaultFraudCheckerClient implements FraudCheckerClient {

    private static final Logger LOG = Logger.getLogger(DefaultFraudCheckerClient.class.getName());

    private final RestTemplate restTemplateWithoutTimeout;

    private final String fraudCheckerServiceUrl;

    @Autowired
    public DefaultFraudCheckerClient(@Value("${fraud-checker.service.url}") String hostname,
                                     @Value("${fraud-checker.service.port}") int port,
                                     RestTemplate restTemplateWithoutTimeout) {
        fraudCheckerServiceUrl = String.format("%s:%d", hostname, port);
        this.restTemplateWithoutTimeout = restTemplateWithoutTimeout;
    }


//  public DefaultFraudCheckerClient(@Value("${fraud-checker.service.url}") String hostname,
//                                   @Value("${fraud-checker.service.port}") int port) {
//    fraudCheckerServiceUri = URI.create(String.format("%s:%d/%s", hostname, port, "check"));
//  }

    public FraudStatus checkFraud(CreditCard creditCard, Money chargedAmount) {
        Map<String, Object> request = new HashMap<>() {{
            put("creditCard", creditCard);
            put("charge", chargedAmount);
        }};
        final URI fraudCheckUri = createFraudCheckUri("/check");
        LOG.info(() -> String.format("Sending /check Request ==> FraudChecker MicroService on %s", fraudCheckUri));
        FraudStatus fraudStatus = restTemplateWithoutTimeout.postForObject(fraudCheckUri, request, FraudStatus.class);
        LOG.info(() -> String.format("Got Response from /check <== FraudChecker MicroService %s", fraudStatus));
        return fraudStatus;
    }

    @Override
    public FraudStatus checkFraudFallback(CreditCard creditCard, Money chargedAmount) {
        throw new UnsupportedOperationException("Fallback is not supported in Default Mode");
    }

    public String ping() {
        final URI fraudCheckerPingUri = createFraudCheckUri("/ping");
        LOG.info(() -> String.format("Sending /ping Request ==> FraudChecker MicroService on %s", fraudCheckerPingUri));
        final String result = restTemplateWithoutTimeout.getForObject(fraudCheckerPingUri, String.class);
        LOG.info(() -> String.format("Got Response from /ping <== FraudChecker MicroService %s", result));
        return result;
    }

    URI createFraudCheckUri(String path) {
        return URI.create(fraudCheckerServiceUrl + path);
    }
}
