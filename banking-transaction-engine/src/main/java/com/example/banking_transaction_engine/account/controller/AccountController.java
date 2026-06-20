package com.example.banking_transaction_engine.account.controller;

import com.example.banking_transaction_engine.account.dto.*;
import com.example.banking_transaction_engine.account.entity.Customer;
import com.example.banking_transaction_engine.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated

public class AccountController {

    private final AccountService accountService;

    @PostMapping("/customers")
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = accountService.createCustomer(request);
        return new ResponseEntity<>(customer, HttpStatus.CREATED);
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public ResponseEntity<AccountResponse> getBalance(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccountBalance(accountNumber);
        return ResponseEntity.ok(response);
    }
}
