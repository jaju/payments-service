package com.tsys.payments.web;

import com.tsys.payments.domain.TransactionReference;
import com.tsys.payments.service.local.PaymentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.logging.Logger;

@Controller("/")
public class PaymentsController {

  private static final Logger LOG = Logger.getLogger(PaymentsController.class.getName());

  private final PaymentsService paymentsService;

  @Autowired
  public PaymentsController(PaymentsService paymentsService) {
    this.paymentsService = paymentsService;
  }

  @RequestMapping
  public String index() {
    return "index.html";
  }

  @GetMapping("ping")
  public ResponseEntity<String> pong() {
    return ResponseEntity.ok(String.format("{ 'PONG' : '%s is running fine!' }", PaymentsController.class.getSimpleName()));
  }

  @PostMapping(value = "pay", consumes = "application/json", produces = "application/json")
  public ResponseEntity<TransactionReference> makePayment(@RequestBody PaymentPayload payload) {
    LOG.info(() -> String.format("Making payment for %s using creditCard %s", payload.order, payload.creditCard));
    return paymentsService.makePayment(payload.order, payload.creditCard)
            .map(ResponseEntity::ok)
            .orElse(new ResponseEntity<>(HttpStatus.BAD_GATEWAY));
  }
}
