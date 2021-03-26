package com.tsys.payments.config;

import com.tsys.payments.repository.TransactionRepository;
import com.tsys.payments.service.local.PaymentsService;
import com.tsys.payments.service.remote.FraudCheckerClient;
import com.tsys.payments.utils.IdGenerator;
import com.tsys.payments.utils.UUIDGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Configuration
@PropertySource("classpath:application-${spring.profiles.active:development}.properties")
public class PaymentsConfig {

    @Bean
    public PaymentsService create(FraudCheckerClient fraudCheckerClient, TransactionRepository transactionRepository, IdGenerator<UUID> uuidGenerator) {
        return new PaymentsService(fraudCheckerClient, transactionRepository, uuidGenerator);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean("uuidGenerator")
    public IdGenerator<UUID> uuidGenerator() {
        return new UUIDGenerator();
    }
}
