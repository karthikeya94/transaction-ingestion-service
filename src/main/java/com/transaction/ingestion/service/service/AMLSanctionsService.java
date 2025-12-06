package com.transaction.ingestion.service.service;

import com.transaction.ingestion.service.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.transaction.ingestion.service.constant.Constant.*;

@Service
@Slf4j
public class AMLSanctionsService {

    public boolean isTransactionCompliant(Transaction transaction) {
        try {
            if (isMerchantSanctioned(transaction.getMerchant())) {
                return false;
            }

            if (transaction.getLocation() != null &&
                isCountrySanctioned(transaction.getLocation().getCountry())) {
                return false;
            }

            return checkExternalAMLService(transaction);
        } catch (Exception e) {
            log.error("Error checking AML compliance for transaction {}: {}",
                transaction.getTransactionId(), e.getMessage(), e);
            return true;
        }
    }

    private boolean isMerchantSanctioned(String merchant) {
        return SANCTIONED_MERCHANTS.contains(merchant);
    }

    private boolean isCountrySanctioned(String country) {
        return SANCTIONED_COUNTRIES.contains(country);
    }

    private boolean checkExternalAMLService(Transaction transaction) {
        try {
            // Simulate calling an external AML service
            // In a real implementation, this would make an HTTP call or gRPC call to an external service
            Thread.sleep(50); // Simulate network delay
            
            // More realistic mock implementation based on transaction data
            if (transaction.getAmount() != null && transaction.getAmount() > 50000) {
                // High-value transactions have a higher chance of being flagged
                return Math.random() > 0.3;
            }
            
            // Regular transactions mostly pass
            return Math.random() > 0.05;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while checking AML service for transaction {}", transaction.getTransactionId(), e);
            return true; // Default to compliant if interrupted
        } catch (Exception e) {
            log.error("Error calling external AML service for transaction {}: {}",
                transaction.getTransactionId(), e.getMessage(), e);
            // In case of service error, we might want to be more conservative
            // but for this implementation we'll default to compliant
            return true;
        }
    }
}