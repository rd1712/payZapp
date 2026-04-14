package com.payzapp.frauddetection.rules;

import com.payzapp.common.events.PaymentSettledEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VelocityRule implements FraudRule {

    private final Map<UUID, List<LocalDateTime>> transactionHistory = new ConcurrentHashMap<>();
    private static final int MAX_TRANSACTIONS = 5;
    private static final int TIME_WINDOW_HOURS = 1;

    @Override
    public boolean isFraudulent(PaymentSettledEvent event) {
        UUID walletId = event.getFromWalletId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(TIME_WINDOW_HOURS);

        // Add current transaction
        transactionHistory.computeIfAbsent(walletId, k -> new ArrayList<>()).add(now);

        // Count transactions within window
        long count = transactionHistory.get(walletId).stream()
                .filter(t -> t.isAfter(windowStart))
                .count();

        return count > MAX_TRANSACTIONS;
    }

    @Override
    public String getRuleName() {
        return "VELOCITY_RULE";
    }
}
