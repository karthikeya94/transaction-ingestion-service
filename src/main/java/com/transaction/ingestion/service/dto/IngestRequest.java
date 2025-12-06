package com.transaction.ingestion.service.dto;

import com.transaction.ingestion.service.model.Location;
import lombok.Data;

import java.time.Instant;

@Data
public class IngestRequest {
    private String customerId;
    private Double amount;
    private String currency;
    private String merchant;
    private String merchantCategory;
    private Instant timestamp;
    private String channel;
    private String device;
    private Location location;
}
