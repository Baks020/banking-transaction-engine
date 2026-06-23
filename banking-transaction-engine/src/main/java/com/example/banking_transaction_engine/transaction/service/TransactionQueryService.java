package com.example.banking_transaction_engine.transaction.service;

import com.example.banking_transaction_engine.account.repository.AccountRepository;
import com.example.banking_transaction_engine.ledger.entity.TransactionJournal;
import com.example.banking_transaction_engine.ledger.repository.LedgerEntryRepository;
import com.example.banking_transaction_engine.ledger.repository.TransactionJournalRepository;
import com.example.banking_transaction_engine.transaction.dto.TransactionSearchRequest;
import com.example.banking_transaction_engine.transaction.specification.TransactionJournalSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionQueryService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionJournalRepository journalRepository;

    @Transactional(readOnly = true) //
    public Page<TransactionJournal> searchTransactions(TransactionSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        var specification = TransactionJournalSpecification.buildSpecification(request);
        return journalRepository.findAll(specification, pageable);
    }
}
