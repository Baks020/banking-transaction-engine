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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferConcurrencyIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Test
    void shouldPreventRaceConditionsAndNegativeBalancesUnderHeavyLoad() throws InterruptedException, ExecutionException {
        // 1. Setup Test Customer
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setFirstName("Concurrency");
        customerRequest.setLastName("Test");
        customerRequest.setEmail("concurrency_" + UUID.randomUUID() + "@bank.com");
        var customer = accountService.createCustomer(customerRequest);

        // 2. Create Source Account with a fixed balance ($100.00)
        CreateAccountRequest sourceReq = new CreateAccountRequest();
        sourceReq.setCustomerId(customer.getId());
        sourceReq.setAccountType(AccountType.CURRENT);
        sourceReq.setCurrency("USD");
        sourceReq.setInitialBalance(BigDecimal.ZERO);
        AccountResponse sourceAcc = accountService.createAccount(sourceReq);

        // Seed exactly $100.00 into the source account
        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber(sourceAcc.getAccountNumber());
        deposit.setAmount(new BigDecimal("100.00"));
        deposit.setCurrency("USD");
        transferService.executeDeposit(deposit);

        // 3. Create Destination Account (Starts at $0.00)
        CreateAccountRequest destReq = new CreateAccountRequest();
        destReq.setCustomerId(customer.getId());
        destReq.setAccountType(AccountType.CURRENT);
        destReq.setCurrency("USD");
        destReq.setInitialBalance(BigDecimal.ZERO);
        AccountResponse destAcc = accountService.createAccount(destReq);

        // 4. Configure Concurrency Framework
        int numberOfParallelRequests = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfParallelRequests);
        CountDownLatch readyLatch = new CountDownLatch(numberOfParallelRequests);
        CountDownLatch startLatch = new CountDownLatch(1);

        // Prepare 10 identical requests of $20.00 each ($200.00 total requested)
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceAccountNumber(sourceAcc.getAccountNumber());
        transferRequest.setDestinationAccountNumber(destAcc.getAccountNumber());
        transferRequest.setAmount(new BigDecimal("20.00"));
        transferRequest.setCurrency("USD");

        List<Callable<TransactionJournal>> tasks = new ArrayList<>();

        for (int i = 0; i < numberOfParallelRequests; i++) {
            tasks.add(() -> {
                readyLatch.countDown(); // Mark worker thread as ready
                readyLatch.await();     // Wait for all sister threads to stand up
                startLatch.await();     // Wait for the main test thread to give the go signal
                return transferService.executeTransfer(transferRequest);
            });
        }

        // 5. Fire all threads simultaneously
        List<Future<TransactionJournal>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(executorService.submit(task));
        }

        // Wait until all threads are fully queued and ready to burst
        readyLatch.await(2, TimeUnit.SECONDS);
        startLatch.countDown(); // Releases all threads at the exact same millisecond

        // Wait for all threads to finish processing or timeout
        executorService.shutdown();
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        // 6. Evaluate the results
        int successfulTransfers = 0;
        int failedTransfers = 0;

        for (Future<TransactionJournal> future : futures) {
            try {
                TransactionJournal journal = future.get();
                if (journal != null && journal.getStatus() == TransactionStatus.COMPLETED) {
                    successfulTransfers++;
                } else {
                    failedTransfers++;
                }
            } catch (Exception e) {
                // Captures database lock timeout exceptions safely as transaction failures
                failedTransfers++;
            }
        }

        System.out.println("--- CONCURRENCY TEST RESULT ---");
        System.out.println("Successful Transfers: " + successfulTransfers);
        System.out.println("Blocked/Failed Transfers: " + failedTransfers);


        // 7. Core Enterprise Assertions
        System.out.println("--- CONCURRENCY TEST RESULT ---");
        System.out.println("Successful Transfers: " + successfulTransfers);
        System.out.println("Blocked/Failed Transfers: " + failedTransfers);

        // Fetch the absolute final balance states from the database
        AccountResponse finalSource = accountService.getAccountBalance(sourceAcc.getAccountNumber());
        AccountResponse finalDest = accountService.getAccountBalance(destAcc.getAccountNumber());

        // RULE 1: The source account balance MUST NEVER drop below zero (Double spending protection)
        assertThat(finalSource.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // RULE 2: Total ecosystem money must balance perfectly ($100 total always preserved)
        BigDecimal totalEcosystemMoney = finalSource.getBalance().add(finalDest.getBalance());
        assertThat(totalEcosystemMoney).isEqualByComparingTo(new BigDecimal("100.0000"));

        // RULE 3: The destination account balance must exactly match the value of successful transfers
        BigDecimal expectedDestBalance = new BigDecimal(successfulTransfers).multiply(new BigDecimal("20.00"));
        assertThat(finalDest.getBalance()).isEqualByComparingTo(expectedDestBalance);

    }
}
