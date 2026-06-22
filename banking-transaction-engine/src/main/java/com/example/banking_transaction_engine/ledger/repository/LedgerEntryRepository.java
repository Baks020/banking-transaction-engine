package com.example.banking_transaction_engine.ledger.repository;

import com.example.banking_transaction_engine.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionReference(String transactionReference);
}
