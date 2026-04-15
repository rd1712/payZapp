package com.payzapp.reportingservice.consumer;

import com.payzapp.common.events.PaymentSettledEvent;
import com.payzapp.reportingservice.model.TransactionRecord;
import com.payzapp.reportingservice.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReportingConsumer {

    private final TransactionRecordRepository transactionRecordRepository;

    @KafkaListener(topics = "payzapp-payment-events", groupId = "reporting-service-group")
    public void handlePaymentSettled(PaymentSettledEvent event) {
        TransactionRecord record = TransactionRecord.builder()
                .paymentId(event.getPaymentId())
                .fromWalletId(event.getFromWalletId())
                .toWalletId(event.getToWalletId())
                .amount(event.getAmount())
                .status(event.getStatus())
                .settledAt(event.getSettledAt())
                .build();

        transactionRecordRepository.save(record);
        System.out.println("Transaction recorded: " + event.getPaymentId());
    }
}