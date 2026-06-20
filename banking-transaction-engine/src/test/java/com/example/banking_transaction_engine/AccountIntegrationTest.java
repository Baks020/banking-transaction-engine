package com.example.banking_transaction_engine;

import com.example.banking_transaction_engine.account.dto.CreateAccountRequest;
import com.example.banking_transaction_engine.account.dto.CreateCustomerRequest;
import com.example.banking_transaction_engine.account.enums.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rolls back database modifications after each test execution runs
class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
    @Test
    void shouldCreateCustomerAndAccountSuccessfully() throws Exception {
        // 1. Create a Customer Request Payload
        String uniqueEmail = "user_" + UUID.randomUUID() + "@enterprise.com";
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setFirstName("John");
        customerRequest.setLastName("Doe");
        customerRequest.setEmail(uniqueEmail);

        // Execute Customer Creation Endpoint Assertion
        String customerResponseJson = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is(uniqueEmail)))
                .andReturn().getResponse().getContentAsString();

        // Extract ID for the next steps
        Integer customerId = objectMapper.readTree(customerResponseJson).get("id").asInt();

        // 2. Create an Account Request Payload tied to the Customer ID
        CreateAccountRequest accountRequest = new CreateAccountRequest();
        accountRequest.setCustomerId(customerId.longValue());
        accountRequest.setAccountType(AccountType.SAVINGS);
        accountRequest.setCurrency("USD");
        accountRequest.setInitialBalance(new BigDecimal("500.00"));

        // Execute Account Creation Endpoint Assertion
        String accountResponseJson = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber", notNullValue()))
                .andExpect(jsonPath("$.balance", is(500.0000))) // Matches Bigdecimal scale(4) specification
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andReturn().getResponse().getContentAsString();

        String accountNum = objectMapper.readTree(accountResponseJson).get("accountNumber").asText();

        // 3. Verify Balance Query Endpoint Works Perfectly
        mockMvc.perform(get("/api/v1/accounts/" + accountNum + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is(accountNum)))
                .andExpect(jsonPath("$.balance", is(500.0000)));
    }

    @Test
    void shouldReturnValidationError_WhenInitialBalanceIsNegative() throws Exception {
        // Create an invalid request using your actual fields
        CreateAccountRequest invalidRequest = new CreateAccountRequest();
        invalidRequest.setCustomerId(1L);
        invalidRequest.setAccountType(AccountType.SAVINGS);
        invalidRequest.setCurrency("USD");
        invalidRequest.setInitialBalance(new BigDecimal("-50.00")); // 👈 Triggers @PositiveOrZero

        mockMvc.perform(post("/api/v1/accounts") // Update to your actual endpoint path
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED")); // Or your error format
    }

}
