package com.transaction.ingestion.service.repository;

import com.transaction.ingestion.service.model.RejectedTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RejectedTransactionRepository extends MongoRepository<RejectedTransaction, String> {
    RejectedTransaction findByTransactionId(String transactionId);
}