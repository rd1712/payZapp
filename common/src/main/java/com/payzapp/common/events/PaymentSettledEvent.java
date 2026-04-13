package com.payzapp.common.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentSettledEvent {
    private UUID paymentId;
    private UUID fromWalletId;
    private UUID toWalletId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime settledAt;
}