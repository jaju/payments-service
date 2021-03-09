package com.tsys.payments.service.local;

import com.tsys.payments.domain.*;
import com.tsys.payments.repository.TransactionRepository;
import com.tsys.payments.service.remote.FraudCheckerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)

@Tag("UnitTest")
class PaymentsServiceSpecs {

  @Mock
  private FraudCheckerClient fraudCheckerClient;

  @Mock
  private TransactionRepository transactionRepository;

  private PaymentsService paymentsService;

  private final Date now = Date.from(Instant.now());
  private final UUID uuid = UUID.nameUUIDFromBytes("TEST".getBytes());
  private final Money amount = new Money(Currency.getInstance("INR"), 1235.45d);
  private final CreditCard validCard = CreditCardBuilder.make()
          .withHolder("Jumping Jack")
          .withIssuingBank("Bank of Test")
          .withValidNumber()
          .withValidCVV()
          .withFutureExpiryDate()
          .build();

  @BeforeEach
  public void setup() {
    paymentsService = new PaymentsService(fraudCheckerClient, transactionRepository) {

      @Override
      UUID createUUID() {
        return uuid;
      }

      @Override
      Date createTransactionDate() {
        return now;
      }
    };
  }
  @Test
  public void returnsAcceptedTransactionReferenceWhenFraudCheckPasses() {
    // Given
    FraudStatus pass = new FraudStatus("pass");
    given(fraudCheckerClient.checkFraud(validCard, amount)).willReturn(pass);

    // When
    final Optional<TransactionReference> transactionReference = paymentsService.makePayment(new Order("TEST-ORDER-ID", List.of(
            new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10))
    ), validCard);

    // Then
    assertThat(transactionReference, is(Optional.of(new TransactionReference(uuid, now, "accepted"))));
  }

  @Test
  public void returnsRejectedTransactionReferenceWhenFraudCheckFails() {
    // Given
    FraudStatus fail = new FraudStatus("fail");
    given(fraudCheckerClient.checkFraud(validCard, amount)).willReturn(fail);

    // When
    final Optional<TransactionReference> transactionReference = paymentsService.makePayment(new Order("TEST-ORDER-ID", List.of(
            new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10))
    ), validCard);

    // Then
    assertThat(transactionReference, is(Optional.of(new TransactionReference(uuid, now, "rejected"))));
  }

  @Test
  public void doesNotReturnTransactionReferenceWhenFraudCheckIsSuspicious() {
    // Given
    FraudStatus fail = new FraudStatus("suspicious");
    given(fraudCheckerClient.checkFraud(validCard, amount)).willReturn(fail);

    // When
    final Optional<TransactionReference> transactionReference = paymentsService.makePayment(new Order("TEST-ORDER-ID", List.of(
            new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10))
    ), validCard);

    // Then
    assertThat(transactionReference, is(Optional.empty()));
  }

  @Test
  public void savesPaymentTransactionWhenFraudCheckPasses() {
    // Given
    FraudStatus pass = new FraudStatus("pass");
    given(fraudCheckerClient.checkFraud(validCard, amount)).willReturn(pass);

    // When
    paymentsService.makePayment(new Order("TEST-ORDER-ID", List.of(
            new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10))
    ), validCard);

    // Then
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  public void savesPaymentTransactionWhenFraudCheckFails() {
    // Given
    FraudStatus fail = new FraudStatus("fail");
    given(fraudCheckerClient.checkFraud(validCard, amount)).willReturn(fail);

    // When
    paymentsService.makePayment(new Order("TEST-ORDER-ID", List.of(
            new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10))
    ), validCard);

    // Then
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  public void doesNotSavePaymentTransactionWhenFraudCheckIsSuspicious() {
    // Given
    FraudStatus fail = new FraudStatus("suspicious");
    given(fraudCheckerClient.checkFraud(validCard, amount)).willReturn(fail);

    // When
    paymentsService.makePayment(new Order("TEST-ORDER-ID", List.of(
            new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10))
    ), validCard);

    // Then
    verify(transactionRepository, never()).save(any(Transaction.class));
  }
}
