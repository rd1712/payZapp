package com.payzapp.paymentservice.model;

public enum PaymentStatus {
    INITIATED,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    REFUNDED
}