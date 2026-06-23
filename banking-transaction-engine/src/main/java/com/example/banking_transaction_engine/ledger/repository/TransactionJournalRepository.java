package com.example.banking_transaction_engine.ledger.repository;

import com.example.banking_transaction_engine.ledger.entity.TransactionJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TransactionJournalRepository extends JpaRepository<TransactionJournal, Long>, JpaSpecificationExecutor<TransactionJournal> {
    Optional<TransactionJournal> findByTransactionReference(String transactionReference);
}
