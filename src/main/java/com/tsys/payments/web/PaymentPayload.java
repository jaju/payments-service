package com.tsys.payments.web;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.Order;

public class PaymentPayload {

  public final Order order;
  public final CreditCard creditCard;

  public PaymentPayload(Order order, CreditCard creditCard) {
    this.order = order;
    this.creditCard = creditCard;
  }
}
