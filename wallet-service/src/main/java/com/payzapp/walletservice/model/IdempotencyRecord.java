package com.payzapp.walletservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")  // ← NEW: TEXT allows longer strings than VARCHAR(255)
    private String response;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}