package com.payzapp.frauddetection.consumer;

import com.payzapp.common.events.PaymentSettledEvent;
import com.payzapp.frauddetection.client.WalletClient;
import com.payzapp.frauddetection.service.FraudDetectionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FraudEventConsumer {

    private final FraudDetectionEngine fraudDetectionEngine;
    private final WalletClient walletClient;

    @KafkaListener(topics = "payzapp-payment-events", groupId = "fraud-detection-group")
    public void handlePaymentSettled(PaymentSettledEvent event) {
        boolean isFraud = fraudDetectionEngine.analyze(event);
        if (isFraud) {
            System.out.println("Freezing wallet: " + event.getFromWalletId());
            walletClient.freezeWallet(event.getFromWalletId());
        }
    }
}