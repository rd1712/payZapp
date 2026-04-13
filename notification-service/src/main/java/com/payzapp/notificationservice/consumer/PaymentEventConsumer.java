package com.payzapp.notificationservice.consumer;

import com.payzapp.common.events.PaymentSettledEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    @KafkaListener(topics = "payzapp-payment-events", groupId = "notification-service-group")
    public void handlePaymentSettled(PaymentSettledEvent event) {
        System.out.println("Payment of " + event.getAmount() + " settled from wallet: " + event.getFromWalletId() + ", To wallet: " + event.getToWalletId());
    }
}