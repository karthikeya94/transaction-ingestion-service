package com.transaction.ingestion.service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "rejected_transaction")
@Data
public class RejectedTransaction {
    
    @Id
    @Field("transactionId")
    private String transactionId;
    
    @Field("customerId")
    private String customerId;
    
    @Field("amount")
    private Double amount;
    
    @Field("currency")
    private String currency;
    
    @Field("merchant")
    private String merchant;
    
    @Field("rejectionReason")
    private String rejectionReason;
    
    @Field("rejectionDetails")
    private RejectionDetails rejectionDetails;
    
    @Field("eventTimestamp")
    private Instant eventTimestamp;
    
    @Field("correlationId")
    private String correlationId;
    
    @Field("createdAt")
    private Instant createdAt;
    
    @Data
    public static class RejectionDetails {
        private Double requestedAmount;
        private Double customerLimit;
        private String validationErrors;
    }
}