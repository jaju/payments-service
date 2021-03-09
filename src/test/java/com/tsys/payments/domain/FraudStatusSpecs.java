package com.tsys.payments.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("UnitTest")
public class FraudStatusSpecs {

  private final Money inr_100_253 = new Money(Currency.getInstance("INR"), 100.253);
  private final UUID id = UUID.nameUUIDFromBytes("TEST-ID".getBytes());
  private final String orderId = "TEST-ORDER-ID";
  private final Date date = new Date();

  @Test
  public void createsAnAcceptedTransactionForAPassedOverallStatus() {
    // Given
    final FraudStatus pass = new FraudStatus("pass");

    // When
    final var transaction = pass.makeTransaction(id, date, orderId, inr_100_253).orElseThrow();

    // Then
    final var expected = new Transaction(id, date, "accepted", orderId, inr_100_253);
    assertThat(transaction, is(expected));
  }

  @Test
  public void createsARejectedTransactionForAFailedOverallStatus() {
    // Given
    final FraudStatus fail = new FraudStatus("fail");

    // When
    final var transaction = fail.makeTransaction(id, date, orderId, inr_100_253).orElseThrow();

    // Then
    final var expected = new Transaction(id, date, "rejected", orderId, inr_100_253);
    assertThat(transaction, is(expected));
  }

  @Test
  public void createsNoTransactionForASuspiciousOverallStatus() {
    // Given
    final FraudStatus suspicious = new FraudStatus("suspicious");

    // When
    final var transaction = suspicious.makeTransaction(id, date, orderId, inr_100_253);

    // Then
    assertThat(transaction, is(Optional.empty()));
  }
}
