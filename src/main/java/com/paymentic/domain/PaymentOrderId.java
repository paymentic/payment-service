package com.paymentic.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentOrderId {
  private String id;
  public PaymentOrderId() {}
  public PaymentOrderId(String id) {
    this.id = id;
  }
  public String getId() {
    return id;
  }

}


