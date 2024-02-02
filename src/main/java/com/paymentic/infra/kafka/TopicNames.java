package com.paymentic.infra.kafka;

public enum TopicNames {
  PAYMENT_PROCESSING("payment-processing");
  private final String name;
  TopicNames(String name) {
    this.name = name;
  }
  public String topicName(){
    return this.name;
  }

}
