package com.example.banking_transaction_engine.account.dto;

import com.example.banking_transaction_engine.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Initial balance cannot be null")
    @PositiveOrZero(message = "Initial balance must be zero or positive")
    private BigDecimal initialBalance;
}
