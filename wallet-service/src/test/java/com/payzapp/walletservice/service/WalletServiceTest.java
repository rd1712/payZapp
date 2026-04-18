package com.payzapp.walletservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payzapp.common.dto.DebitRequest;
import com.payzapp.common.dto.DebitResponse;
import com.payzapp.walletservice.exception.WalletDoesntExistException;
import com.payzapp.walletservice.model.IdempotencyRecord;
import com.payzapp.walletservice.model.Wallet;
import com.payzapp.walletservice.model.WalletStatus;
import com.payzapp.walletservice.repository.IdempotencyRepository;
import com.payzapp.walletservice.repository.LedgerEntryRepository;
import com.payzapp.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    WalletRepository walletRepository;

    @Mock
    LedgerEntryRepository ledgerEntryRepository;

    @Mock
    IdempotencyRepository idempotencyRepository;

    @InjectMocks
    WalletService walletService;

    @Test
    void debit_shouldThrowException_whenInsufficientBalance() {
        // Arrange
        UUID walletId = UUID.randomUUID();

        Wallet wallet = Wallet.builder()
                .walletId(walletId)
                .balance(new BigDecimal("500"))
                .status(WalletStatus.ACTIVE)
                .build();

        DebitRequest request = new DebitRequest(walletId, new BigDecimal("1000"), "test-key-001");

        when(idempotencyRepository.findById("test-key-001")).thenReturn(Optional.empty());
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // Act + Assert
        assertThrows(RuntimeException.class, () -> walletService.debit(request));

        // Verify balance was never saved
        verify(walletRepository, never()).save(any());
    }
@Test
    void debit_shouldThrowException_whenAmountIsNegative() {

        UUID walletId = UUID.randomUUID();

        DebitRequest request = new DebitRequest(walletId, new BigDecimal(-100), "test-key-001");

        assertThrows(RuntimeException.class, () -> walletService.debit(request));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void debit_shouldThrowException_whenWalletNotFound(){

        UUID walletId = UUID.randomUUID();



        when(idempotencyRepository.findById("test-key-001")).thenReturn(Optional.empty());
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        DebitRequest debitRequest = new DebitRequest(walletId,new BigDecimal(100),"test-key-001");

        assertThrows (WalletDoesntExistException.class,() -> walletService.debit(debitRequest));

        verify(walletRepository, never()).save(any());

    }

    @Test
    void debit_shouldThrowException_whenWalletIsFrozen(){

        UUID walletId = UUID.randomUUID();

        Wallet wallet = Wallet.builder().walletId(walletId).status(WalletStatus.FROZEN).build();

        when(idempotencyRepository.findById("test-key-001")).thenReturn(Optional.empty());
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        DebitRequest request = new DebitRequest(walletId,new BigDecimal(100),"test-key-001");

        assertThrows(RuntimeException.class,()->walletService.debit(request));

        verify(walletRepository,never()).save(any());
    }
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void debit_shouldReturnCachedResponse_whenIdempotencyKeyAlreadyExists() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        DebitResponse cachedResponse = DebitResponse.builder()
                .walletId(walletId)
                .transactionId(transactionId)
                .amount(new BigDecimal("100"))
                .remainingBalance(new BigDecimal("900"))
                .status("COMPLETED")
                .build();

        String responseJson = objectMapper.writeValueAsString(cachedResponse);

        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .idempotencyKey("test-key-001")
                .status("COMPLETED")
                .response(responseJson)
                .build();

        DebitRequest request = new DebitRequest(walletId, new BigDecimal("100"), "test-key-001");

        when(idempotencyRepository.findById("test-key-001")).thenReturn(Optional.of(existingRecord));

        // Act
        DebitResponse response = walletService.debit(request);

        // Assert
        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(walletRepository, never()).findById(any()); // wallet never looked up
    }

    @Test
    void debit_shouldDebitSuccessfully_andCreateLedgerEntries() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();

        Wallet wallet = Wallet.builder()
                .walletId(walletId)
                .balance(new BigDecimal("1000"))
                .status(WalletStatus.ACTIVE)
                .build();

        DebitRequest request = new DebitRequest(walletId, new BigDecimal("100"), "test-key-001");

        when(idempotencyRepository.findById("test-key-001")).thenReturn(Optional.empty());
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenReturn(wallet);
        when(idempotencyRepository.save(any())).thenReturn(IdempotencyRecord.builder().build());

        // Act
        DebitResponse response = walletService.debit(request);

        // Assert
        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(walletRepository, times(1)).save(any());
        verify(ledgerEntryRepository, times(2)).save(any());
    }
}

