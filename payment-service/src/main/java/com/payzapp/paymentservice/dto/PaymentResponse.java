package com.payzapp.paymentservice.dto;

import com.payzapp.paymentservice.model.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private UUID fromWalletId;
    private UUID toWalletId;
    private BigDecimal amount;
    private PaymentStatus status;
}