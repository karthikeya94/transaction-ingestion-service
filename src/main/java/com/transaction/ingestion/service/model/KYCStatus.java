package com.transaction.ingestion.service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "customer_kyc")
@Data
public class KYCStatus {
    
    @Id
    @Field("customerId")
    private String customerId;
    
    @Field("verificationStatus")
    private String verificationStatus;
    
    @Field("verificationDate")
    private Instant verificationDate;
    
    @Field("expiryDate")
    private Instant expiryDate;
    
    @Field("documentType")
    private String documentType;
    
    @Field("documentNumber")
    private String documentNumber;
    
    @Field("verifiedBy")
    private String verifiedBy;
    
    @Field("createdAt")
    private Instant createdAt;
    
    @Field("updatedAt")
    private Instant updatedAt;
}