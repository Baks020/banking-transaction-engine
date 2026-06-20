package com.example.banking_transaction_engine.account.dto;

import com.example.banking_transaction_engine.account.enums.AccountStatus;
import com.example.banking_transaction_engine.account.enums.AccountType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AccountResponse {
    private String accountNumber;
    private Long customerId;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private String currency;
}
