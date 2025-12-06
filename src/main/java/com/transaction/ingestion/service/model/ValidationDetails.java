package com.transaction.ingestion.service.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
public class ValidationDetails {
    @Field("structuralCheck")
    private String structuralCheck;
    
    @Field("businessCheck")
    private String businessCheck;
    
    @Field("kycStatus")
    private String kycStatus;
    
    @Field("velocityFlag")
    private boolean velocityFlag;
    
    @Field("sanctionsCheck")
    private String sanctionsCheck;
    
    @Field("validatedAt")
    private Instant validatedAt;
}
