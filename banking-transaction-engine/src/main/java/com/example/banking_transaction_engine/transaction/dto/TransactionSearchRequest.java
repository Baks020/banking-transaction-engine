package com.example.banking_transaction_engine.transaction.dto;

import com.example.banking_transaction_engine.ledger.enums.TransactionStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransactionSearchRequest {
    private String accountNumber;
    private String transactionReference;
    private TransactionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Pagination attributes to protect server memory bounds
    private int page = 0;
    private int size = 20;
}
