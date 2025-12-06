package com.transaction.ingestion.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Error error;

    @Data
    @AllArgsConstructor
    public static class Error {
        private String code;
        private String message;
        private List<Violation> violations;
    }

    @Data
    @AllArgsConstructor
    public static class Violation {
        private String field;
        private String message;
    }
}
