package com.payzapp.paymentservice.kafka;

import com.payzapp.common.events.PaymentSettledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, PaymentSettledEvent> kafkaTemplate;

    @Value("${app.kafka.topic.payment-events}")
    private String paymentEventsTopic;

    public void publishPaymentSettled(PaymentSettledEvent event) {
        kafkaTemplate.send(paymentEventsTopic, event.getPaymentId().toString(), event);
    }
}
