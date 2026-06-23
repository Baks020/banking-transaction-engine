package com.example.banking_transaction_engine.transaction;

import com.example.banking_transaction_engine.account.dto.AccountResponse;
import com.example.banking_transaction_engine.account.dto.CreateAccountRequest;
import com.example.banking_transaction_engine.account.dto.CreateCustomerRequest;
import com.example.banking_transaction_engine.account.enums.AccountType;
import com.example.banking_transaction_engine.account.service.AccountService;
import com.example.banking_transaction_engine.ledger.entity.TransactionJournal;
import com.example.banking_transaction_engine.ledger.enums.TransactionStatus;
import com.example.banking_transaction_engine.transaction.dto.DepositRequest;
import com.example.banking_transaction_engine.transaction.dto.TransferRequest;
import com.example.banking_transaction_engine.transaction.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rolls back the seeded audit data rows cleanly after execution finishes
class TransactionHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String completedTransferRef;
    private String failedTransferRef;

    @BeforeEach
    void setUpTestData() {
        // 1. Setup a Test Customer
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setFirstName("Reporting");
        customerRequest.setLastName("Test");
        customerRequest.setEmail("reporting_" + UUID.randomUUID() + "@bank.com");
        var customer = accountService.createCustomer(customerRequest);

        // 2. Create Source Account
        CreateAccountRequest sourceReq = new CreateAccountRequest();
        sourceReq.setCustomerId(customer.getId());
        sourceReq.setAccountType(AccountType.CURRENT);
        sourceReq.setCurrency("USD");
        sourceReq.setInitialBalance(BigDecimal.ZERO);
        AccountResponse sourceAcc = accountService.createAccount(sourceReq);
        this.sourceAccountNumber = sourceAcc.getAccountNumber();

        // 3. Create Destination Account
        CreateAccountRequest destReq = new CreateAccountRequest();
        destReq.setCustomerId(customer.getId());
        destReq.setAccountType(AccountType.SAVINGS);
        destReq.setCurrency("USD");
        destReq.setInitialBalance(BigDecimal.ZERO);
        AccountResponse destAcc = accountService.createAccount(destReq);
        this.destinationAccountNumber = destAcc.getAccountNumber();

        // 4. Seed Transaction A: $500.00 Deposit (COMPLETED)
        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber(sourceAccountNumber);
        deposit.setAmount(new BigDecimal("500.00"));
        deposit.setCurrency("USD");
        transferService.executeDeposit(deposit);

        // 5. Seed Transaction B: $100.00 Transfer (COMPLETED)
        TransferRequest successfulTransfer = new TransferRequest();
        successfulTransfer.setSourceAccountNumber(sourceAccountNumber);
        successfulTransfer.setDestinationAccountNumber(destinationAccountNumber);
        successfulTransfer.setAmount(new BigDecimal("100.00"));
        successfulTransfer.setCurrency("USD");
        TransactionJournal journalSuccess = transferService.executeTransfer(successfulTransfer);
        this.completedTransferRef = journalSuccess.getTransactionReference();

        // 6. Seed Transaction C: Overdraft Transfer Request (FAILED due to Insufficient Funds)
        TransferRequest failingTransfer = new TransferRequest();
        failingTransfer.setSourceAccountNumber(sourceAccountNumber);
        failingTransfer.setDestinationAccountNumber(destinationAccountNumber);
        failingTransfer.setAmount(new BigDecimal("9999.00")); // Intentionally forces business logic break
        failingTransfer.setCurrency("USD");
        TransactionJournal journalFail = transferService.executeTransfer(failingTransfer);
        this.failedTransferRef = journalFail.getTransactionReference();
    }

    @Test
    void shouldFilterTransactionsByStatus() throws Exception {
        // Query endpoint filtering for FAILED items specifically
        mockMvc.perform(get("/api/v1/transactions")
                        .param("status", "FAILED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].transactionReference", is(failedTransferRef)))
                .andExpect(jsonPath("$.content[0].status", is("FAILED")))
                .andExpect(jsonPath("$.content[0].failureReason", containsString("Insufficient funds")));
    }


        @Test
    void shouldFindSpecificTransactionByUniqueReference() throws Exception {
        // Query endpoint using direct exact lookup via the tracking token reference
        mockMvc.perform(get("/api/v1/transactions")
                        .param("transactionReference", completedTransferRef)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionReference", is(completedTransferRef)))
                .andExpect(jsonPath("$.content[0].status", is("COMPLETED")))
                .andExpect(jsonPath("$.content[0].amount", is(100.0000)));
    }

    @Test
    void shouldFilterTransactionsByDateRange() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String startString = now.minusHours(1).toString();
        String endString = now.plusHours(1).toString();

        // Query endpoint validating bounded historical constraints
        mockMvc.perform(get("/api/v1/transactions")
                        .param("startDate", startString)
                        .param("endDate", endString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Spring Data Pages wrap arrays in a "content" field container
                .andExpect(jsonPath("$.content", is(notNullValue())));
    }
}
