package com.transaction.ingestion.service.repository;

import com.transaction.ingestion.service.model.KYCStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KYCStatusRepository extends MongoRepository<KYCStatus, String> {
    KYCStatus findByCustomerId(String customerId);
}