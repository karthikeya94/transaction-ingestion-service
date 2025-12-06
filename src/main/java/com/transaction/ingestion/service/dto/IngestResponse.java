package com.transaction.ingestion.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class IngestResponse {
    private String transactionId;
    private String status;
    private String message;
    private Instant timestamp;
}
