package com.payzapp.frauddetection.service;

import com.payzapp.common.events.PaymentSettledEvent;
import com.payzapp.frauddetection.rules.FraudRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudDetectionEngine {

    private final List<FraudRule> fraudRules;

    public boolean analyze(PaymentSettledEvent event) {
        for (FraudRule rule : fraudRules) {
            if (rule.isFraudulent(event)) {
                System.out.println("Fraud detected by rule: " + rule.getRuleName()
                        + " for payment: " + event.getPaymentId());
                return true;
            }
        }
        return false;
    }
}