package com.payzapp.walletservice.dto;

import com.payzapp.walletservice.model.Wallet;
import com.payzapp.walletservice.model.WalletStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class WalletResponse {
    private UUID walletId;
    private UUID userId;
    private BigDecimal balance;
    private WalletStatus status;
}
