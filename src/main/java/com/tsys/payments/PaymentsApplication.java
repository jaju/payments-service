package com.tsys.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Steps to make Payments microservice Retryable
 * <p>
 * 1. Add in build.gradle, the dependency -
 * <p>
 * implementation 'org.springframework.retry:spring-retry'
 * implementation 'org.springframework:spring-aspects'
 * <p>
 * 2. Add Enable Retry Annotation – @EnableRetry in the main application
 * class
 * <p>
 * 3. Modify the remote FraudChecker implementation to add the
 *
 * @Retryable annotation with maxAttempts = 2 or 4 and backoff = 500 ms
 * Play with this for a while to understand how retry works and then
 * go to step #4
 * <p>
 * 4. Adding @Recover annotation -
 */
@SpringBootApplication
@EnableRetry
public class PaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
//		final SpringApplication application = new SpringApplication(PaymentsApplication.class)
//		application.setDefaultProperties(Collections.singletonMap("server.port", "9000"));
//		application.run(args);
    }

}
