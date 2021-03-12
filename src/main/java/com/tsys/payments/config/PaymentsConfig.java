package com.tsys.payments.config;

import com.tsys.payments.repository.TransactionRepository;
import com.tsys.payments.service.local.PaymentsService;
import com.tsys.payments.service.remote.FraudCheckerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@PropertySource("classpath:application-${spring.profiles.active:development}.properties")
public class PaymentsConfig {

    @Bean
    public PaymentsService create(FraudCheckerClient fraudCheckerClient, TransactionRepository transactionRepository) {
        return new PaymentsService(fraudCheckerClient, transactionRepository);
    }

//  @Bean
//  @Qualifier("fraud_checker_retry")
//  @ConditionalOnExpression("#{${features.resiliency.latency_control.strategy} eq 'retry'}")
//  public FraudChecker withRetryStrategy(@Value("${fraud-checker.service.host}") String hostname,
//                                        @Value("${fraud-checker.service.port}") int port) {
//    return new FraudCheckerWithRetryPattern(hostname, port);
//  }
//
//  @Bean
//  @Qualifier("fraud_checker_cb")
//  @ConditionalOnExpression("#{${features.resiliency.latency_control.strategy} eq 'circuit_breaker'}")
//  public FraudChecker withCircuitBreakerStrategy(@Value("${fraud-checker.service.host}") String hostname,
//                                        @Value("${fraud-checker.service.port}") int port) {
//    return new FraudCheckerWithCircuitBreakerPattern(hostname, port);
//  }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder,
                                     @Value("${latency_control.timeout.connect}") long connectTimeoutMillis,
                                     @Value("${latency_control.timeout.read}") long readTimeoutMillis) {
        // Timeout Pattern - The goal is to avoid unbounded waiting times for
        // responses and thus treating every request as failed where no response
        // was received within the timeout.
        // Many HTTP clients have a default timeout configured.  Here we are using
        // Spring RestTemplate to timeout after 3 seconds.
        return restTemplateBuilder
                // The connection timeout is the timeout in making the initial
                // connection; i.e. completing the TCP connection handshake.
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMillis))
                // The read timeout is the timeout on waiting to read data1. Specifically,
                // if the server fails to send a byte <timeout> seconds after the last byte,
                // a read timeout error will be raised.
                .setReadTimeout(Duration.ofMillis(readTimeoutMillis))
                .build();

        // NOTE:
        // =====
        // A connection timeout set to "infinity" means that the
        // connection attempt can potentially block for ever. There is no
        // infinite loop, but the attempt to connect can be unblocked by
        // another thread closing the socket.
        //
        // A read timeout set to "infinity" means that a call to read
        // on the socket stream may block for ever. Once again there is
        // no infinite loop, but the read can be unblocked by a Thread.interrupt()
        // call, closing the socket, and (of course) the other end sending data
        // or closing the connection.
    }

    @Bean
    public RestTemplate restTemplateWithoutTimeout(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}
