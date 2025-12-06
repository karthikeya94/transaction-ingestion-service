package com.transaction.ingestion.service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "customer")
@Data
public class Customer {
    
    @Id
    @Field("customerId")
    private String customerId;
    
    @Field("name")
    private String name;
    
    @Field("email")
    private String email;
    
    @Field("status")
    private String status;
    
    @Field("tier")
    private String tier;
    
    @Field("dailyLimit")
    private Double dailyLimit;
    
    @Field("transactionLimit")
    private Double transactionLimit;
    
    @Field("blacklisted")
    private Boolean blacklisted;
    
    @Field("createdAt")
    private Instant createdAt;
    
    @Field("updatedAt")
    private Instant updatedAt;
}