package com.payzapp.walletservice.repository;

import com.payzapp.walletservice.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {

}