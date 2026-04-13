package com.payzapp.walletservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DebitResponse {
    private UUID walletId;
    private UUID transactionId;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private String status;
}