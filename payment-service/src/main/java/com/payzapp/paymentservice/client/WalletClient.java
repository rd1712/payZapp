package com.payzapp.paymentservice.client;

import com.payzapp.common.dto.DebitRequest;
import com.payzapp.common.dto.DebitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service", url = "http://localhost:8082")
public interface WalletClient {

    @PostMapping("/api/wallet/debit")
    DebitResponse debit(@RequestBody DebitRequest request);

    @PostMapping("/api/wallet/credit")
    DebitResponse credit(@RequestBody DebitRequest request);
}