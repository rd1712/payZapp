package com.payzapp.reportingservice.controller;

import com.payzapp.reportingservice.model.TransactionRecord;
import com.payzapp.reportingservice.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final TransactionRecordRepository transactionRecordRepository;

    @GetMapping("/transactions")
    public List<TransactionRecord> getAllTransactions() {
        return transactionRecordRepository.findAll();
    }

    @GetMapping("/transactions/{walletId}")
    public List<TransactionRecord> getTransactionsByWallet(@PathVariable("walletId") UUID walletId) {
        return transactionRecordRepository.findByFromWalletId(walletId);
    }
}