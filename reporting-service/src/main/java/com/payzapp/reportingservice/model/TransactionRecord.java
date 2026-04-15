package com.payzapp.reportingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_records")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransactionRecord {

    @Id
    private UUID paymentId;

    @Column(nullable = false)
    private UUID fromWalletId;

    @Column(nullable = false)
    private UUID toWalletId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime settledAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}