package com.payzapp.frauddetection.rules;

import com.payzapp.common.events.PaymentSettledEvent;

public interface FraudRule {
    boolean isFraudulent(PaymentSettledEvent event);
    String getRuleName();
}
