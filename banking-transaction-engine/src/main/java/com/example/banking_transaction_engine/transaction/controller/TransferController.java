package com.example.banking_transaction_engine.transaction.controller;

import com.example.banking_transaction_engine.ledger.entity.TransactionJournal;
import com.example.banking_transaction_engine.transaction.dto.DepositRequest;
import com.example.banking_transaction_engine.transaction.dto.TransferRequest;
import com.example.banking_transaction_engine.transaction.service.TransactionQueryService;
import com.example.banking_transaction_engine.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import com.example.banking_transaction_engine.transaction.dto.TransactionSearchRequest;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Validated
public class TransferController {

    private final TransferService transferService;
    private final TransactionQueryService transactionQueryService;

    /**
     * Endpoint to inject external funds into a simulator account
     */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionJournal> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionJournal journal = transferService.executeDeposit(request);
        if (journal.getStatus() == com.example.banking_transaction_engine.ledger.enums.TransactionStatus.FAILED) {
            return new ResponseEntity<>(journal, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return new ResponseEntity<>(journal, HttpStatus.OK);
    }

    /**
     * Endpoint to safely move balances between internally registered accounts
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionJournal> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionJournal journal = transferService.executeTransfer(request);
        if (journal.getStatus() == com.example.banking_transaction_engine.ledger.enums.TransactionStatus.FAILED) {
            return new ResponseEntity<>(journal, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return new ResponseEntity<>(journal, HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<Page<TransactionJournal>> getTransactionHistory(@ModelAttribute TransactionSearchRequest request) {
        Page<TransactionJournal> history = transactionQueryService.searchTransactions(request);
        return ResponseEntity.ok(history);
    }
}
