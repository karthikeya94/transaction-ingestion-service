package com.transaction.ingestion.service.service;

import com.transaction.ingestion.service.model.Customer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerService {


    public boolean isCustomerActive(Customer customer) {
        try {
            return customer != null && "ACTIVE".equals(customer.getStatus());
        } catch (Exception e) {
            log.error("Error checking customer active status for customer ID {}: {}", customer.getCustomerId(), e.getMessage(), e);
            return false;
        }
    }

    public boolean isCustomerBlacklisted(Customer customer) {
        try {
            return customer != null && customer.getBlacklisted() != null && customer.getBlacklisted();
        } catch (Exception e) {
            log.error("Error checking customer blacklist status for customer ID {}: {}", customer.getCustomerId(), e.getMessage(), e);
            return false;
        }
    }

    public double getCustomerLimit(Customer customer) {
        try {
            if (customer != null) {
                if (customer.getTransactionLimit() != null) {
                    return customer.getTransactionLimit();
                }
                if (customer.getTier() != null) {
                    return switch (customer.getTier()) {
                        case "PREMIUM" -> 50000.00;
                        case "STANDARD" -> 10000.00;
                        case "BASIC" -> 5000.00;
                        default -> 1000.00;
                    };
                }
            }
            return 1000.00;
        } catch (Exception e) {
            log.error("Error getting customer limit for customer ID {}: {}", customer.getCustomerId(), e.getMessage(), e);
            return 1000.00;
        }
    }
}
