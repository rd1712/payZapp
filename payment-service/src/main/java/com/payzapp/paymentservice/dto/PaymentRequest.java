package com.payzapp.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PaymentRequest {
    @NotNull
    private UUID fromWalletId;
    @NotNull
    private UUID toWalletId;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private String idempotencyKey;
}