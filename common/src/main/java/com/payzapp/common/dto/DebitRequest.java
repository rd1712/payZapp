package com.payzapp.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DebitRequest {
    @NotNull
    private UUID walletId;
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String idempotencyKey;
}


