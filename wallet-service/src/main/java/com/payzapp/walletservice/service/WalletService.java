package com.payzapp.walletservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.payzapp.common.dto.DebitRequest;
import com.payzapp.common.dto.DebitResponse;
import com.payzapp.walletservice.dto.WalletResponse;
import com.payzapp.walletservice.exception.WalletAlreadyExistsException;
import com.payzapp.walletservice.exception.WalletDoesntExistException;
import com.payzapp.walletservice.model.*;
import com.payzapp.walletservice.repository.IdempotencyRepository;
import com.payzapp.walletservice.repository.LedgerEntryRepository;
import com.payzapp.walletservice.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ObjectMapper objectMapper;

    public WalletResponse createWallet(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException("Wallet Already Exists");
        }

            Wallet wallet = walletRepository.save(Wallet.builder()
                    .userId(userId)
                    .status(WalletStatus.ACTIVE)
                    .balance(BigDecimal.ZERO)
                    .build());

            return WalletResponse.builder()
                    .walletId(wallet.getWalletId())
                    .userId(wallet.getUserId())
                    .balance(wallet.getBalance())
                    .status(wallet.getStatus())
                    .build();



    }

    public BigDecimal getBalance(UUID userId){
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(()-> new WalletDoesntExistException("Wallet does not exist"));

        return wallet.getBalance();

    }
    @Transactional
    public DebitResponse debit(DebitRequest request) {

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        // Step 1: Check idempotency
        Optional<IdempotencyRecord> existing = idempotencyRepository.findById(request.getIdempotencyKey());
        if (existing.isPresent()) {
            if (existing.get().getStatus().equals("COMPLETED")) {
                try {
                    return objectMapper.readValue(existing.get().getResponse(), DebitResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error reading cached response");
                }
            } else {
                throw new RuntimeException("Request is already being processed, please retry");
            }
        }

        // Step 2: Store PROCESSING
        idempotencyRepository.save(IdempotencyRecord.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .status("PROCESSING")
                .build());

        // Step 3: Find wallet
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletDoesntExistException("Wallet not found"));

        // Step 4: Check sufficient balance
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Step 5: Debit balance
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        // Step 6: Create ledger entry
        UUID transactionId = UUID.randomUUID();
        ledgerEntryRepository.save(LedgerEntry.builder()
                .walletId(wallet.getWalletId())
                .transactionId(transactionId)
                .type(EntryType.DEBIT)
                .amount(request.getAmount())
                .description("Debit transaction")
                .build());

        // Credit system account
        ledgerEntryRepository.save(LedgerEntry.builder()
                .walletId(UUID.fromString("00000000-0000-0000-0000-000000000000")) // system account UUID
                .transactionId(transactionId)
                .type(EntryType.CREDIT)
                .amount(request.getAmount())
                .description("System credit for debit transaction")
                .build());

        // Step 7: Build response
        DebitResponse response = DebitResponse.builder()
                .walletId(wallet.getWalletId())
                .transactionId(transactionId)
                .amount(request.getAmount())
                .remainingBalance(wallet.getBalance())
                .status("COMPLETED")
                .build();

        // Step 8: Update idempotency to COMPLETED
        try {
            String responseJson = new ObjectMapper().writeValueAsString(response);
            idempotencyRepository.save(IdempotencyRecord.builder()
                    .idempotencyKey(request.getIdempotencyKey())
                    .status("COMPLETED")
                    .response(responseJson)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Error saving idempotency record");
        }

        return response;
    }

    @Transactional
    public DebitResponse credit(DebitRequest request) {

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        // Step 1: Check idempotency
        Optional<IdempotencyRecord> existing = idempotencyRepository.findById(request.getIdempotencyKey());
        if (existing.isPresent()) {
            if (existing.get().getStatus().equals("COMPLETED")) {
                try {
                    return objectMapper.readValue(existing.get().getResponse(), DebitResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error reading cached response");
                }
            } else {
                throw new RuntimeException("Request is already being processed, please retry");
            }
        }

        // Step 2: Store PROCESSING
        idempotencyRepository.save(IdempotencyRecord.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .status("PROCESSING")
                .build());

        // Step 3: Find wallet
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletDoesntExistException("Wallet not found"));

        // Step 4: Credit balance
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        // Step 5: Create ledger entries
        UUID transactionId = UUID.randomUUID();

        // Credit user wallet
        ledgerEntryRepository.save(LedgerEntry.builder()
                .walletId(wallet.getWalletId())
                .transactionId(transactionId)
                .type(EntryType.CREDIT)
                .amount(request.getAmount())
                .description("Credit transaction")
                .build());

        // Debit system account
        ledgerEntryRepository.save(LedgerEntry.builder()
                .walletId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .transactionId(transactionId)
                .type(EntryType.DEBIT)
                .amount(request.getAmount())
                .description("System debit for credit transaction")
                .build());

        // Step 6: Build response
        DebitResponse response = DebitResponse.builder()
                .walletId(wallet.getWalletId())
                .transactionId(transactionId)
                .amount(request.getAmount())
                .remainingBalance(wallet.getBalance())
                .status("COMPLETED")
                .build();

        // Step 7: Update idempotency to COMPLETED
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            idempotencyRepository.save(IdempotencyRecord.builder()
                    .idempotencyKey(request.getIdempotencyKey())
                    .status("COMPLETED")
                    .response(responseJson)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Error saving idempotency record");
        }

        return response;
    }
}
