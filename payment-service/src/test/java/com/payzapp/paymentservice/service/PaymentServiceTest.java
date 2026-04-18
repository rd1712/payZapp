package com.payzapp.paymentservice.service;

import com.payzapp.common.dto.DebitResponse;
import com.payzapp.common.events.PaymentSettledEvent;
import com.payzapp.paymentservice.client.WalletClient;
import com.payzapp.paymentservice.dto.PaymentRequest;
import com.payzapp.paymentservice.dto.PaymentResponse;
import com.payzapp.paymentservice.kafka.KafkaProducerService;
import com.payzapp.paymentservice.model.Payment;
import com.payzapp.paymentservice.model.PaymentStatus;
import com.payzapp.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    WalletClient walletClient;

    @Mock
    KafkaProducerService kafkaProducerService;

    @InjectMocks
    PaymentService paymentService;

    private PaymentRequest buildRequest() {
        return new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100"),
                "test-idempotency-key"
        );
    }

    private Payment buildPayment(PaymentRequest request, PaymentStatus status) {
        return Payment.builder()
                .paymentId(UUID.randomUUID())
                .fromWalletId(request.getFromWalletId())
                .toWalletId(request.getToWalletId())
                .amount(request.getAmount())
                .status(status)
                .idempotencyKey(request.getIdempotencyKey())
                .build();
    }

    // Test 1 — Idempotency: same request returns cached response
    @Test
    void initiatePayment_shouldReturnCachedResponse_whenIdempotencyKeyExists() {
        PaymentRequest request = buildRequest();
        Payment existingPayment = buildPayment(request, PaymentStatus.SETTLED);

        when(paymentRepository.findByIdempotencyKey("test-idempotency-key"))
                .thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.initiatePayment(request);

        assertEquals(PaymentStatus.SETTLED, response.getStatus());
        verify(walletClient, never()).debit(any());
        verify(walletClient, never()).credit(any());
    }

    // Test 2 — Happy path: payment settles successfully
    @Test
    void initiatePayment_shouldSettlePayment_whenEverythingSucceeds() {
        PaymentRequest request = buildRequest();
        Payment savedPayment = buildPayment(request, PaymentStatus.INITIATED);
        DebitResponse debitResponse = DebitResponse.builder()
                .status("COMPLETED")
                .build();

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(walletClient.debit(any())).thenReturn(debitResponse);
        when(walletClient.credit(any())).thenReturn(debitResponse);

        PaymentResponse response = paymentService.initiatePayment(request);

        assertEquals(PaymentStatus.SETTLED, response.getStatus());
        verify(walletClient, times(1)).debit(any());
        verify(walletClient, times(1)).credit(any());

        // Verify Kafka event was published with correct data
        ArgumentCaptor<PaymentSettledEvent> captor = ArgumentCaptor.forClass(PaymentSettledEvent.class);
        verify(kafkaProducerService).publishPaymentSettled(captor.capture());
        PaymentSettledEvent event = captor.getValue();
        assertEquals(request.getFromWalletId(), event.getFromWalletId());
        assertEquals(request.getToWalletId(), event.getToWalletId());
        assertEquals(request.getAmount(), event.getAmount());
        assertEquals("SETTLED", event.getStatus());
    }

    // Test 3 — Debit fails: payment should be FAILED, no credit attempted
    @Test
    void initiatePayment_shouldFailPayment_whenDebitFails() {
        PaymentRequest request = buildRequest();
        Payment savedPayment = buildPayment(request, PaymentStatus.INITIATED);

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(walletClient.debit(any())).thenThrow(new RuntimeException("Insufficient balance"));

        PaymentResponse response = paymentService.initiatePayment(request);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(walletClient, never()).credit(any()); // credit never attempted
        verify(kafkaProducerService, never()).publishPaymentSettled(any()); // event never published
    }

    // Test 4 — Credit fails: compensation should run, debit should be reversed
    @Test
    void initiatePayment_shouldCompensate_whenCreditFails() {
        PaymentRequest request = buildRequest();
        Payment savedPayment = buildPayment(request, PaymentStatus.INITIATED);
        DebitResponse debitResponse = DebitResponse.builder()
                .status("COMPLETED")
                .build();

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(walletClient.debit(any())).thenReturn(debitResponse);
        when(walletClient.credit(any())).thenThrow(new RuntimeException("Credit failed"));

        PaymentResponse response = paymentService.initiatePayment(request);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(kafkaProducerService, never()).publishPaymentSettled(any());
    }

    // Test 5 — Kafka event published only on SETTLED
    @Test
    void initiatePayment_shouldPublishKafkaEvent_onlyWhenSettled() {
        PaymentRequest request = buildRequest();
        Payment savedPayment = buildPayment(request, PaymentStatus.INITIATED);
        DebitResponse debitResponse = DebitResponse.builder()
                .status("COMPLETED")
                .build();

        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(walletClient.debit(any())).thenReturn(debitResponse);
        when(walletClient.credit(any())).thenReturn(debitResponse);

        paymentService.initiatePayment(request);

        verify(kafkaProducerService, times(1)).publishPaymentSettled(any());
    }
}