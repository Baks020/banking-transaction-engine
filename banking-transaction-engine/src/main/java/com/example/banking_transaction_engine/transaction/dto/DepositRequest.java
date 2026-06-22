package com.example.banking_transaction_engine.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotBlank(message = "Account number is mandatory")
    private String accountNumber;

    @NotNull(message = "Deposit amount is required")
    @Positive(message = "Deposit amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;
}
