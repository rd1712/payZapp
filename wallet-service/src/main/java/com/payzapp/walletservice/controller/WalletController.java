package com.payzapp.walletservice.controller;

import com.payzapp.common.dto.DebitRequest;
import com.payzapp.common.dto.DebitResponse;
import com.payzapp.walletservice.dto.WalletResponse;
import com.payzapp.walletservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    @PostMapping("/api/wallet/create")
    public WalletResponse createWallet(){

        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return walletService.createWallet(userId);

    }

    @GetMapping("/api/wallet/balance")
    public BigDecimal getBalance(){
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return walletService.getBalance(userId);
    }
    @PostMapping("/api/wallet/debit")
    public DebitResponse debit(@Valid @RequestBody DebitRequest request) {
        return walletService.debit(request);
    }

    @PostMapping("/api/wallet/credit")
    public DebitResponse credit(@Valid @RequestBody DebitRequest request) {
        return walletService.credit(request);
    }
}
