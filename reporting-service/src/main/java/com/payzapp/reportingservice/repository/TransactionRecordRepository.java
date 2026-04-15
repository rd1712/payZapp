package com.payzapp.reportingservice.repository;

import com.payzapp.reportingservice.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {
    List<TransactionRecord> findByFromWalletId(UUID fromWalletId);
    List<TransactionRecord> findByToWalletId(UUID toWalletId);
}