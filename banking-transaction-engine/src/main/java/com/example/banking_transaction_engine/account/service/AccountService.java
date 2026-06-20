package com.example.banking_transaction_engine.account.service;

import com.example.banking_transaction_engine.account.dto.*;
import com.example.banking_transaction_engine.account.entity.Account;
import com.example.banking_transaction_engine.account.entity.Customer;
import com.example.banking_transaction_engine.account.repository.AccountRepository;
import com.example.banking_transaction_engine.account.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public Customer createCustomer(CreateCustomerRequest request) {
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Customer with this email already exists");
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();

        return customerRepository.save(customer);
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String uniqueAccountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .accountNumber(uniqueAccountNumber)
                .customer(customer)
                .accountType(request.getAccountType())
                .balance(request.getInitialBalance().setScale(4))
                .currency(request.getCurrency().toUpperCase())
                .build();

        Account savedAccount = accountRepository.save(account);
        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return mapToResponse(account);
    }

    /**
     * Avoiding predictable serial IDs.
     * Generates a 12-digit string format: Branch Prefix (3) + Random Numeric Token (9)
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long token = 100000000L + random.nextInt(900000000);
            accountNumber = "100" + token;
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomer().getId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();
    }


}
