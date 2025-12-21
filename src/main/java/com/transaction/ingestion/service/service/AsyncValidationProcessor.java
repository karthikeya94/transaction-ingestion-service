package com.transaction.ingestion.service.service;

import com.riskplatform.common.entity.Transaction;
import com.riskplatform.common.entity.ValidationDetails;
import com.transaction.ingestion.service.model.TransactionEvent;
import com.transaction.ingestion.service.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class AsyncValidationProcessor {

    private final TransactionRepository transactionRepository;
    private final AdvancedValidationService advancedValidationService;

    @KafkaListener(topics = "${kafka.topics.transaction-received}", groupId = "async-validation-group")
    public void processTransactionReceivedEvent(TransactionEvent event) {
        try {
            log.info("Processing transaction received event for transaction ID: {}", event.getTransactionId());

            Transaction transaction = buildTransactionFromEvent(event);

            ValidationDetails validationDetails = advancedValidationService.performAdvancedValidation(transaction);
            log.debug("Validation completed for transaction ID: {}. Risk flags: {}. validation: {}",
                    event.getTransactionId(), transaction.getRiskFlags(), validationDetails);

            transaction.setUpdatedAt(Instant.now());
            transactionRepository.save(transaction);

            log.info("Completed async validation for transaction ID: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing transaction received event for transaction ID: {}", event.getTransactionId(),
                    e);
        }
    }

    private Transaction buildTransactionFromEvent(TransactionEvent event) {
        return transactionRepository.findByTransactionId(event.getTransactionId()).get();
    }
}