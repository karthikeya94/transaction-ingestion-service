package com.transaction.ingestion.service.model;

import com.riskplatform.common.model.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String eventId;
    private String transactionId;
    private String customerId;
    private Double amount;
    private String currency;
    private String merchant;
    private String merchantCategory;
    private Instant timestamp;
    private String channel;
    private String device;
    private Location location;
    private String eventType;
    private Instant eventTimestamp;
    private String correlationId;
    private String rejectionReason;
    private RejectionDetails rejectionDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectionDetails {
        private Double requestedAmount;
        private Double customerLimit;
    }
}
