package com.tsys.payments.domain;

// Address Verification Service (AVS)
// AVS is an effective security measure to detect online fraud.
// When customers purchase items, they need to provide their billing
// address and ZIP code. An AVS will check if this address matches with
// what the card issuing bank has on file.
// Part of a card-not-present (CNP) transaction, the payment gateway
// can send a request for user verification to the issuing bank.
// The AVS responds with a code that would help the merchant understand
// if the transaction is has a full AVS match.
// If they don’t match, more investigation should be carried out by
// checking the CVV (Card Verification Value), email address, IP address
// on the transaction or allow your payment gateway to decline the
// transaction.
//
// Card Verification Value (CVV)
// The CVV (or Card Verification Code ) is the 3 or 4-digit code that
// is on every credit card. The code should never be stored on the
// merchant’s database. A CVV filter acts as an added security measure,
// allowing only the cardholder to use the card since it is available
// only on the printed card. If an order is placed on your website and
// the CVV does not match, you should allow your payment gateway to
// decline the transaction.  While making a card-not-present
// transaction (online, email or telephone orders), merchant gets the
// required card information from the customer to verify the transaction.
// Friendly fraud, is a risk associated with CNP transactions, that can
// lead to a chargeback. Enabling a CVV filter helps merchants fight
// fraud and reduce chargebacks.
//
// Device Identification
// Device identification analysis the computer rather than the person
// who is visiting your website. It profiles the operating system,
// internet connection and browser to gauge if the online transaction
// has to be approved, flagged or declined. All devices (phones,
// computers, tablets, etc) have a unique device fingerprint, similar
// to the fingerprints of people, that helps identify fraudulent
// patterns and assess risk if any.

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class FraudStatus {
  public final String overall;

  public static final FraudStatus EMPTY = new FraudStatus("") {
    @Override
    public String toString() {
      return "<<FraudStatus.EMPTY>>";
    }

    @Override
    public Optional<Transaction> makeTransaction(UUID id, Date date, String orderId, Money value) {
      return Optional.empty();
    }
  };

  @Deprecated
  private FraudStatus() {
    this("");
  }

  public FraudStatus(String overall) {
    this.overall = overall;
  }

  @Override
  public String toString() {
    return "FraudStatus{" +
            "overall='" + overall + '\'' +
            '}';
  }

  public Optional<Transaction> makeTransaction(UUID id, Date date, String orderId, Money value) {
    if (overall.equals("pass"))
      return Optional.of(new Transaction(id, date,"accepted", orderId, value));

    if (overall.equals("fail"))
      return Optional.of(new Transaction(id, date,"rejected", orderId, value));

    return Optional.empty();
  }
}
