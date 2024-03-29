package com.paymentic.domain.application;

import com.paymentic.domain.Checkout;
import com.paymentic.domain.CheckoutId;
import com.paymentic.domain.PaymentOrder;
import com.paymentic.domain.events.CheckoutClosedEvent;
import com.paymentic.domain.events.IdempotencyKeyRequestUsage;
import com.paymentic.domain.events.PaymentCreatedEvent;
import com.paymentic.domain.events.PaymentOrderEvent;
import com.paymentic.domain.events.data.CheckoutData;
import com.paymentic.domain.events.data.PaymentOrderData;
import com.paymentic.domain.events.publisher.PaymentEventsPublisher;
import com.paymentic.domain.repositories.CheckoutRepository;
import com.paymentic.domain.shared.BuyerInfo;
import com.paymentic.domain.shared.CardInfo;
import com.paymentic.domain.shared.SellerInfo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.kafka.common.protocol.types.Field.Str;
import org.openapitools.model.PaymentOrders;
import org.openapitools.model.PaymentRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService implements ApplicationListener<CheckoutClosedEvent> {
  private final CheckoutRepository checkoutRepository;
  private final ApplicationEventPublisher publisher;
  private final PaymentEventsPublisher eventsBridge;
  private final IdempotencyKeyService idempotencyKeyService;
  public CheckoutService(CheckoutRepository checkoutRepository, ApplicationEventPublisher publisher,
      PaymentEventsPublisher eventsBridge, IdempotencyKeyService idempotencyKeyService) {
    this.checkoutRepository = checkoutRepository;
    this.publisher = publisher;
    this.eventsBridge = eventsBridge;
    this.idempotencyKeyService = idempotencyKeyService;
  }
  @Transactional
  public Checkout processPaymentRequest(PaymentRequest request, String idempotencyKey) {
    IdempotencyKeyRequestUsage idempotencyKeyRequestUsage = new IdempotencyKeyRequestUsage(this, idempotencyKey);
    idempotencyKeyService.handleUsage(idempotencyKeyRequestUsage);
    Checkout checkout = createCheckout(request, idempotencyKey);
    List<PaymentOrderData> paymentOrderDataList = processPaymentOrders(request, checkout);
    var checkoutData = new CheckoutData(checkout.getId(),checkout.getBuyerInfo(),checkout.getCardInfo(),idempotencyKey,
        LocalDateTime.now());
    paymentOrderDataList.forEach(order -> {
      PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent(checkoutData, order);
      eventsBridge.paymentCreated(paymentCreatedEvent);
    });
    return checkout;
  }
  private Checkout createCheckout(PaymentRequest request, String idempotencyKey) {
    BuyerInfo buyerInfo = new BuyerInfo(request.getBuyerInfo().getDocument(), request.getBuyerInfo().getName());
    CardInfo cardInfo = new CardInfo(request.getCreditCardInfo().getCardInfo(), request.getCreditCardInfo().getToken());
    Checkout checkout = Checkout.newCheckoutInitiated(request.getCheckoutId(), buyerInfo, cardInfo, idempotencyKey);
    checkoutRepository.save(checkout);
    return checkout;
  }
  private List<PaymentOrderData> processPaymentOrders(PaymentRequest request, Checkout checkout) {
    List<PaymentOrderData> paymentOrderDataList = new ArrayList<>();
    for (PaymentOrders paymentOrder : request.getPaymentOrders()) {
      PaymentOrder payment = PaymentOrder.newPaymentInitiated(paymentOrder.getAmount(), paymentOrder.getCurrency(),
          new CheckoutId(request.getCheckoutId()), new SellerInfo(paymentOrder.getSellerAccount()));
      publisher.publishEvent(new PaymentOrderEvent(this, new CheckoutId(request.getCheckoutId()), payment));
      paymentOrderDataList.add(new PaymentOrderData(payment.getId(), payment.getAmount(), payment.getCurrency(),
          payment.getStatus(), new SellerInfo(paymentOrder.getSellerAccount()), payment.getIdempotencyKey()));
    }
    return paymentOrderDataList;
  }
  @Override
  public void onApplicationEvent(CheckoutClosedEvent event) {
    Optional<Checkout> checkout = this.checkoutRepository.findById(
        UUID.fromString(event.getCheckoutId().getId()));
    checkout.ifPresent(chk -> this.checkoutRepository.save(chk.markDone()));
  }
  public Checkout getById(String id){
    Optional<Checkout> checkout = this.checkoutRepository.findById(UUID.fromString(id));
    return checkout.get();
  }

}
