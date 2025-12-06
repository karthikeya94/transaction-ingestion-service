package com.transaction.ingestion.service.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class BatchStatus {
    private String batchId;
    private String status;
    private int totalRecords;
    private int processedRecords;
    private int acceptedRecords;
    private int rejectedRecords;
    private Instant startTime;
    private Instant completionTime;
    private Map<String, Integer> rejectionSummary;
}
