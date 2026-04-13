package com.payzapp.paymentservice.service;

import com.payzapp.common.dto.DebitRequest;
import com.payzapp.paymentservice.client.WalletClient;
import com.payzapp.paymentservice.dto.PaymentRequest;
import com.payzapp.paymentservice.dto.PaymentResponse;
import com.payzapp.paymentservice.model.Payment;
import com.payzapp.paymentservice.model.PaymentStatus;
import com.payzapp.paymentservice.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletClient walletClient;

    private static final Map<PaymentStatus, List<PaymentStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(PaymentStatus.INITIATED, List.of(PaymentStatus.AUTHORIZED, PaymentStatus.FAILED));
        VALID_TRANSITIONS.put(PaymentStatus.AUTHORIZED, List.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED));
        VALID_TRANSITIONS.put(PaymentStatus.CAPTURED, List.of(PaymentStatus.SETTLED, PaymentStatus.FAILED));
        VALID_TRANSITIONS.put(PaymentStatus.SETTLED, List.of(PaymentStatus.REFUNDED));
        VALID_TRANSITIONS.put(PaymentStatus.FAILED, List.of());
        VALID_TRANSITIONS.put(PaymentStatus.REFUNDED, List.of());
    }

    private void validateTransition(PaymentStatus current, PaymentStatus next) {
        if (!VALID_TRANSITIONS.get(current).contains(next)) {
            throw new RuntimeException("Invalid transition from " + current + " to " + next);
        }
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        // Step 1: Create payment record
        Payment payment = paymentRepository.save(Payment.builder()
                .fromWalletId(request.getFromWalletId())
                .toWalletId(request.getToWalletId())
                .amount(request.getAmount())
                .status(PaymentStatus.INITIATED)
                .idempotencyKey(request.getIdempotencyKey())
                .build());

        try {
            // Step 2: Debit Wallet A
            String debitKey = payment.getPaymentId() + "-debit";
            walletClient.debit(new DebitRequest(request.getFromWalletId(), request.getAmount(), debitKey));

            // Update state
            validateTransition(payment.getStatus(), PaymentStatus.AUTHORIZED);
            payment.setStatus(PaymentStatus.AUTHORIZED);
            paymentRepository.save(payment);

            // Step 3: Credit Wallet B
            String creditKey = payment.getPaymentId() + "-credit";
            walletClient.credit(new DebitRequest(request.getToWalletId(), request.getAmount(), creditKey));

            // Update state
            validateTransition(payment.getStatus(), PaymentStatus.CAPTURED);
            payment.setStatus(PaymentStatus.CAPTURED);
            paymentRepository.save(payment);

            // Step 4: Settle
            validateTransition(payment.getStatus(), PaymentStatus.SETTLED);
            payment.setStatus(PaymentStatus.SETTLED);
            paymentRepository.save(payment);

        } catch (Exception e) {
            System.out.println("Payment failed: " + e.getMessage());
            e.printStackTrace();
            compensate(payment);
        }

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .fromWalletId(payment.getFromWalletId())
                .toWalletId(payment.getToWalletId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }

    private void compensate(Payment payment) {
        try {
            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                // Undo credit to Wallet B
                String undoCreditKey = payment.getPaymentId() + "-undo-credit";
                walletClient.debit(new DebitRequest(payment.getToWalletId(), payment.getAmount(), undoCreditKey));

                // Undo debit from Wallet A
                String undoDebitKey = payment.getPaymentId() + "-undo-debit";
                walletClient.credit(new DebitRequest(payment.getFromWalletId(), payment.getAmount(), undoDebitKey));

            } else if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                // Only undo debit from Wallet A
                String undoDebitKey = payment.getPaymentId() + "-undo-debit";
                walletClient.credit(new DebitRequest(payment.getFromWalletId(), payment.getAmount(), undoDebitKey));
            }
            // INITIATED → nothing to undo

        } catch (Exception e) {
            // Compensation itself failed — this is a serious problem
            // In production: alert on-call engineer, manual intervention needed
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }
}
