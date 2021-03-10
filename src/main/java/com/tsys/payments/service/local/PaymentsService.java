package com.tsys.payments.service.local;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.Order;
import com.tsys.payments.domain.Transaction;
import com.tsys.payments.domain.TransactionReference;
import com.tsys.payments.repository.TransactionRepository;
import com.tsys.payments.service.remote.FraudCheckerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PaymentsService {

    private static final Logger LOG = Logger.getLogger(PaymentsService.class.getName());

    private final FraudCheckerClient fraudCheckerClient;

    private final TransactionRepository transactionRepository;

    @Autowired
    //  @Qualifier("fraud_checker_cb")
    //  @Qualifier("fraud_checker_retry")
    public PaymentsService(FraudCheckerClient fraudCheckerClient, TransactionRepository transactionRepository) {
        this.fraudCheckerClient = fraudCheckerClient;
        this.transactionRepository = transactionRepository;
    }

    public Optional<TransactionReference> makePayment(Order order, CreditCard creditCard) {
        final var amount = order.amount;
        LOG.info(() -> String.format("Order Amount = %s, Total Items = %s", order.amount, order.totalItems()));
        final var transaction = fraudCheckerClient
                .checkFraud(creditCard, amount)
                .makeTransaction(createUUID(), createTransactionDate(), order.id, amount);

        final var transactionReference = transaction.map(Transaction::reference);
        transaction.ifPresent(t -> transactionRepository.save(t));
        return transactionReference;
    }

    Date createTransactionDate() {
        return Date.from(Instant.now());
    }

    UUID createUUID() {
        return UUID.randomUUID();
    }
}
