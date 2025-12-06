package com.transaction.ingestion.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RejectedResponse {
    private Error error;

    @Data
    @AllArgsConstructor
    public static class Error {
        private String code;
        private String message;
        private String reason;
        private Details details;
    }

    @Data
    @AllArgsConstructor
    public static class Details {
        private String customerId;
        private Double requestedAmount;
        private Double customerLimit;
        private Instant timestamp;
    }
}
