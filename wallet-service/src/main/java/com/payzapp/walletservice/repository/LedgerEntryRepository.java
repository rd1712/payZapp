package com.payzapp.walletservice.repository;

import com.payzapp.walletservice.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}