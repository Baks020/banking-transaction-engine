package com.example.banking_transaction_engine.transaction.service;

import com.example.banking_transaction_engine.account.entity.Account;
import com.example.banking_transaction_engine.account.repository.AccountRepository;
import com.example.banking_transaction_engine.ledger.entity.LedgerEntry;
import com.example.banking_transaction_engine.ledger.entity.TransactionJournal;
import com.example.banking_transaction_engine.ledger.enums.EntryType;
import com.example.banking_transaction_engine.ledger.enums.TransactionStatus;
import com.example.banking_transaction_engine.ledger.repository.LedgerEntryRepository;
import com.example.banking_transaction_engine.ledger.repository.TransactionJournalRepository;
import com.example.banking_transaction_engine.transaction.dto.TransferRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.banking_transaction_engine.transaction.dto.DepositRequest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionJournalRepository journalRepository;

    @Transactional
    public TransactionJournal executeTransfer(TransferRequest request) {
        // 1. Generate an un-guessable unique transaction tracking reference
        String txRef = UUID.randomUUID().toString();

        // 2. Fetch base accounts to determine their ID values (No lock yet)
        Account sourceRef = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account destRef = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if (sourceRef.getAccountNumber().equals(destRef.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }

        // 3. Prevent Database Deadlocks by always locking accounts in ascending primary key numerical order
        Account sourceAccount;
        Account destAccount;
        if (sourceRef.getId() < destRef.getId()) {
            sourceAccount = accountRepository.findWithLockByAccountNumber(sourceRef.getAccountNumber()).get();
            destAccount = accountRepository.findWithLockByAccountNumber(destRef.getAccountNumber()).get();
        } else {
            destAccount = accountRepository.findWithLockByAccountNumber(destRef.getAccountNumber()).get();
            sourceAccount = accountRepository.findWithLockByAccountNumber(sourceRef.getAccountNumber()).get();
        }

        // 4. Initialise Journal entry record state
        TransactionJournal journal = TransactionJournal.builder()
                .transactionReference(txRef)
                .sourceAccountNumber(sourceAccount.getAccountNumber())
                .destinationAccountNumber(destAccount.getAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(TransactionStatus.PENDING)
                .build();
        journal = journalRepository.save(journal);

        // 5. Business Validation Rules
        if (!sourceAccount.getCurrency().equals(request.getCurrency()) ||
                !destAccount.getCurrency().equals(request.getCurrency())) {
            return markTransactionFailed(journal, "Currency mismatch across transaction boundaries");
        }

        // Prevent Negative Balances Rule
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            return markTransactionFailed(journal, "Insufficient funds present inside source account");
        }

        // 6. Mutate balances safely inside isolated transactional context memory boundaries
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        destAccount.setBalance(destAccount.getBalance().add(request.getAmount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        // 7. Write Balancing Double-Entry Transaction Ledger Logs
        LedgerEntry debitLeg = LedgerEntry.builder()
                .transactionReference(txRef)
                .accountNumber(sourceAccount.getAccountNumber())
                .entryType(EntryType.DEBIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .build();

        LedgerEntry creditLeg = LedgerEntry.builder()
                .transactionReference(txRef)
                .accountNumber(destAccount.getAccountNumber())
                .entryType(EntryType.CREDIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .build();

        ledgerEntryRepository.save(debitLeg);
        ledgerEntryRepository.save(creditLeg);

        // 8. Commit Status Success
        journal.setStatus(TransactionStatus.COMPLETED);
        return journalRepository.save(journal);
    }

    private TransactionJournal markTransactionFailed(TransactionJournal journal, String reason) {
        journal.setStatus(TransactionStatus.FAILED);
        journal.setFailureReason(reason);
        return journalRepository.save(journal);
    }


    @Transactional
    public TransactionJournal executeDeposit(DepositRequest request) {
        String txRef = UUID.randomUUID().toString();

        // 1. Pessimistic lock the account immediately to secure the row balance modification
        Account account = accountRepository.findWithLockByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        // 2. Initialize the Transaction Journal record
        TransactionJournal journal = TransactionJournal.builder()
                .transactionReference(txRef)
                .sourceAccountNumber("EXTERNAL_DEPOSIT") // Represents external cash influx
                .destinationAccountNumber(account.getAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(TransactionStatus.PENDING)
                .build();
        journal = journalRepository.save(journal);

        // 3. Currency Match Validation
        if (!account.getCurrency().equals(request.getCurrency().toUpperCase())) {
            return markTransactionFailed(journal, "Currency mismatch across transaction boundaries");
        }

        // 4. Update internal account memory state
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        // 5. Generate balancing Credit Entry log
        LedgerEntry creditLeg = LedgerEntry.builder()
                .transactionReference(txRef)
                .accountNumber(account.getAccountNumber())
                .entryType(EntryType.CREDIT)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .build();
        ledgerEntryRepository.save(creditLeg);

        // 6. Complete and save journal metadata
        journal.setStatus(TransactionStatus.COMPLETED);
        return journalRepository.save(journal);
    }

}
