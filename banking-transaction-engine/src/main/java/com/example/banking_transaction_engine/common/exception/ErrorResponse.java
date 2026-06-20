package com.example.banking_transaction_engine.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String message;
    private String errorCode;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;
}
