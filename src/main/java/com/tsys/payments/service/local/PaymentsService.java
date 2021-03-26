package com.tsys.payments.service.local;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.Order;
import com.tsys.payments.domain.Transaction;
import com.tsys.payments.domain.TransactionReference;
import com.tsys.payments.repository.TransactionRepository;
import com.tsys.payments.service.remote.FraudCheckerClient;
import com.tsys.payments.utils.IdGenerator;
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
    private IdGenerator<UUID> uuidGenerator;

    @Autowired
    public PaymentsService(FraudCheckerClient fraudCheckerClient, TransactionRepository transactionRepository, IdGenerator<UUID> uuidGenerator) {
        this.fraudCheckerClient = fraudCheckerClient;
        this.transactionRepository = transactionRepository;
        this.uuidGenerator = uuidGenerator;
    }

    public Optional<TransactionReference> makePayment(Order order, CreditCard creditCard) {
        final var amount = order.amount;
        final var transaction = fraudCheckerClient
                .checkFraud(creditCard, amount)
                .makeTransaction(uuidGenerator.generate(), createTransactionDate(), order.id, amount);

        final var transactionReference = transaction.map(Transaction::reference);
        transaction.ifPresent(t -> transactionRepository.save(t));
        LOG.info(() -> String.format("Total Items = %s", transactionRepository));
        return transactionReference;
    }

    Date createTransactionDate() {
        return Date.from(Instant.now());
    }
    public void setUUIDGenerator(IdGenerator<UUID> uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }
}
