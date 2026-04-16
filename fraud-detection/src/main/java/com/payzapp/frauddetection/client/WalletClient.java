package com.payzapp.frauddetection.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.UUID;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/api/wallet/freeze/{walletId}")
    void freezeWallet(@PathVariable("walletId")  UUID walletId);
}