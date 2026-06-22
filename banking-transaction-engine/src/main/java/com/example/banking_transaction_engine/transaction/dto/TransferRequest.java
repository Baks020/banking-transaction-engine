package com.example.banking_transaction_engine.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Source account number is mandatory")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is mandatory")
    private String destinationAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @Positive(message = "Transfer amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;
}
