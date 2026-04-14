package com.payzapp.frauddetection.rules;

import com.payzapp.common.events.PaymentSettledEvent;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class HighAmountRule implements FraudRule {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000");

    @Override
    public boolean isFraudulent(PaymentSettledEvent event) {
        return event.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0;
    }

    @Override
    public String getRuleName() {
        return "HIGH_AMOUNT_RULE";
    }
}