package com.transaction.ingestion.service.repository;

import com.transaction.ingestion.service.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);
    java.util.List<Transaction> findByCustomerIdAndTimestampAfterOrderByTimestampDesc(String customerId, java.time.Instant timestamp);
}
