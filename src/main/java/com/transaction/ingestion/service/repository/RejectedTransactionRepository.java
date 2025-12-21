package com.transaction.ingestion.service.repository;

import com.riskplatform.common.entity.RejectedTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RejectedTransactionRepository extends MongoRepository<RejectedTransaction, String> {
    RejectedTransaction findByTransactionId(String transactionId);
}