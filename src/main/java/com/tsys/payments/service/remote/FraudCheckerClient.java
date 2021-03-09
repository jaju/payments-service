package com.tsys.payments.service.remote;

import com.tsys.payments.domain.CreditCard;
import com.tsys.payments.domain.FraudStatus;
import com.tsys.payments.domain.Money;
import org.springframework.stereotype.Service;

@Service
public interface FraudCheckerClient {
  FraudStatus checkFraud(CreditCard creditCard, Money chargedAmount);

  FraudStatus checkFraudFallback(CreditCard creditCard, Money chargedAmount);
}
