package com.transaction.ingestion.service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BulkUploadRequest {
    private String fileUrl;
    private String format;
    private int totalRecords;
    private Map<String, String> metadata;
}
