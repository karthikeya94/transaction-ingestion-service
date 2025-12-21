package com.transaction.ingestion.service.repository;

import com.riskplatform.common.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    Customer findByCustomerId(String customerId);
}