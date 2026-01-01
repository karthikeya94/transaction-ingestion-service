package com.transaction.ingestion.service.service;

import com.transaction.ingestion.service.config.ValidationProperties;
import com.transaction.ingestion.service.model.KYCStatus;
import com.riskplatform.common.entity.Transaction;
import com.riskplatform.common.event.TransactionEvent;
import com.riskplatform.common.entity.ValidationDetails;
import com.transaction.ingestion.service.repository.KYCStatusRepository;
import com.transaction.ingestion.service.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class AdvancedValidationService {

    private final KYCStatusRepository kycStatusRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ValidationProperties validationProperties;

    @Autowired
    private AMLSanctionsService amlSanctionsService;

    public ValidationDetails performAdvancedValidation(Transaction transaction) {
        ValidationDetails validationDetails = new ValidationDetails();
        List<String> riskFlags = new ArrayList<>();

        String kycStatus = checkKYCStatus(transaction.getCustomerId());
        validationDetails.setKycStatus(kycStatus);
        if ("EXPIRED".equals(kycStatus)) {
            riskFlags.add("KYC_EXPIRED");
        }

        // Velocity check disabled (was using Redis)
        validationDetails.setVelocityFlag(false);

        boolean patternDeviation = checkTransactionPattern(transaction);
        if (patternDeviation) {
            riskFlags.add("PATTERN_DEVIATION");
        }

        String sanctionsCheck = checkSanctions(transaction);
        validationDetails.setSanctionsCheck(sanctionsCheck);
        if (!"PASSED".equals(sanctionsCheck)) {
            riskFlags.add("SANCTIONS_RISK");
        }

        validationDetails.setStructuralCheck("PASSED");
        validationDetails.setBusinessCheck("PASSED");
        validationDetails.setValidatedAt(Instant.now());

        transaction.setRiskFlags(riskFlags);
        transaction.setValidationDetails(validationDetails);

        if (riskFlags.isEmpty()) {
            publishTransactionValidatedEvent(transaction);
        } else {
            publishTransactionValidationFailedEvent(transaction, riskFlags);
        }

        return validationDetails;
    }

    private String checkKYCStatus(String customerId) {
        try {
            KYCStatus kycStatus = kycStatusRepository.findByCustomerId(customerId);
            if (kycStatus == null) {
                return "NOT_FOUND";
            }

            if ("VERIFIED".equals(kycStatus.getVerificationStatus())) {
                if (kycStatus.getExpiryDate() != null &&
                        kycStatus.getExpiryDate().isBefore(Instant.now())) {
                    return "EXPIRED";
                }
                return "VERIFIED";
            }

            return kycStatus.getVerificationStatus();
        } catch (Exception e) {
            log.error("Error checking KYC status for customer {}: {}", customerId, e.getMessage(), e);
            return "ERROR";
        }
    }

    private boolean checkTransactionPattern(Transaction transaction) {
        try {
            String customerId = transaction.getCustomerId();
            Instant now = Instant.now();

            // Get recent transactions for pattern analysis (last 30 days)
            Instant thirtyDaysAgo = now.minusSeconds(30 * 24 * 60 * 60L);
            List<Transaction> recentTransactions = transactionRepository
                    .findByCustomerIdAndTimestampAfterOrderByTimestampDesc(
                            customerId, thirtyDaysAgo);

            // If this is the first transaction, no pattern deviation
            if (recentTransactions.isEmpty()) {
                return false;
            }

            // Check for velocity pattern deviation
            if (checkVelocityDeviation(transaction, recentTransactions)) {
                return true;
            }

            // Check for amount pattern deviation
            if (checkAmountDeviation(transaction, recentTransactions)) {
                return true;
            }

            // Check for merchant/category pattern deviation
            if (checkMerchantDeviation(transaction, recentTransactions)) {
                return true;
            }

            // Check for channel/device pattern deviation
            if (checkChannelDeviation(transaction, recentTransactions)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking transaction pattern for customer {}: {}", transaction.getCustomerId(),
                    e.getMessage(), e);
            return false;
        }
    }

    private String checkSanctions(Transaction transaction) {
        try {
            boolean isCompliant = amlSanctionsService.isTransactionCompliant(transaction);

            if (isCompliant) {
                return "PASSED";
            } else {
                return "FLAGGED";
            }
        } catch (Exception e) {
            log.error("Error checking sanctions for transaction {}: {}", transaction.getTransactionId(), e.getMessage(),
                    e);
            return "ERROR";
        }
    }

    private boolean checkVelocityDeviation(Transaction transaction, List<Transaction> recentTransactions) {
        if (!validationProperties.getRules().isEnableVelocityCheck()) {
            return false;
        }

        // Calculate transactions per hour for this customer
        Instant oneHourAgo = Instant.now().minusSeconds(60 * 60L);
        long recentCount = recentTransactions.stream()
                .filter(t -> t.getTimestamp().isAfter(oneHourAgo))
                .count();

        // Check if this transaction exceeds the hourly limit
        int maxTransactionsPerHour = validationProperties.getRules().getMaxTransactionsPerHour() != null
                ? validationProperties.getRules().getMaxTransactionsPerHour()
                : 50;

        return recentCount >= maxTransactionsPerHour;
    }

    private boolean checkAmountDeviation(Transaction transaction, List<Transaction> recentTransactions) {
        if (recentTransactions.size() < 5) {
            // Not enough history to establish a pattern
            return false;
        }

        // Calculate average and standard deviation of recent transaction amounts
        double sum = recentTransactions.stream()
                .map(Transaction::getAmount)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();
        double average = sum / recentTransactions.size();

        double squaredDifferenceSum = recentTransactions.stream()
                .map(Transaction::getAmount)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .map(amt -> Math.pow(amt - average, 2))
                .sum();
        double standardDeviation = Math.sqrt(squaredDifferenceSum / recentTransactions.size());

        // Check if current transaction amount is more than 2 standard deviations from
        // the average
        // This indicates a significant deviation from the customer's normal spending
        // pattern
        double currentAmount = transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0;
        double deviation = Math.abs(currentAmount - average);
        return deviation > (2 * standardDeviation);
    }

    private boolean checkMerchantDeviation(Transaction transaction, List<Transaction> recentTransactions) {
        if (recentTransactions.size() < 10 || transaction.getMerchant() == null) {
            // Not enough history or no merchant info
            return false;
        }

        // Count occurrences of each merchant in recent transactions
        Map<String, Long> merchantFrequency = recentTransactions.stream()
                .filter(t -> t.getMerchant() != null)
                .collect(Collectors.groupingBy(Transaction::getMerchant, Collectors.counting()));

        // If no merchant frequency data, can't determine pattern
        if (merchantFrequency.isEmpty()) {
            return false;
        }

        // Calculate percentage of transactions at this merchant
        long totalTransactions = merchantFrequency.values().stream().mapToLong(Long::longValue).sum();
        long merchantCount = merchantFrequency.getOrDefault(transaction.getMerchant(), 0L);
        double merchantPercentage = (double) merchantCount / totalTransactions;

        // If this merchant is unusual (less than 5% of transactions), flag it
        // unless this is a common merchant category
        if (merchantPercentage < 0.05) {
            // Check if this is a common merchant category
            Set<String> commonCategories = Set.of("GROCERY", "GAS_STATION", "RESTAURANT", "COFFEE_SHOP");
            String category = transaction.getMerchantCategory();
            return !commonCategories.contains(category);
        }

        return false;
    }

    private boolean checkChannelDeviation(Transaction transaction, List<Transaction> recentTransactions) {
        if (recentTransactions.size() < 5 || transaction.getChannel() == null) {
            // Not enough history or no channel info
            return false;
        }

        // Count occurrences of each channel in recent transactions
        Map<String, Long> channelFrequency = recentTransactions.stream()
                .filter(t -> t.getChannel() != null)
                .collect(Collectors.groupingBy(Transaction::getChannel, Collectors.counting()));

        // If no channel frequency data, can't determine pattern
        if (channelFrequency.isEmpty()) {
            return false;
        }

        // Calculate percentage of transactions on this channel
        long totalTransactions = channelFrequency.values().stream().mapToLong(Long::longValue).sum();
        long channelCount = channelFrequency.getOrDefault(transaction.getChannel(), 0L);
        double channelPercentage = (double) channelCount / totalTransactions;

        // If this channel is unusual (less than 10% of transactions), flag it
        return channelPercentage < 0.10;
    }

    private void publishTransactionValidatedEvent(Transaction transaction) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("evt-" + transaction.getTransactionId() + "-2")
                .transactionId(transaction.getTransactionId())
                .customerId(transaction.getCustomerId())
                .amount(transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0)
                .currency(transaction.getCurrency())
                .merchant(transaction.getMerchant())
                .merchantCategory(transaction.getMerchantCategory())
                .timestamp(transaction.getTimestamp())
                .channel(transaction.getChannel())
                .device(transaction.getDevice() != null ? transaction.getDevice().getType() : null)
                .location(transaction.getLocation())
                .eventTypeString("TransactionValidated")
                .eventTimestamp(Instant.now())
                .correlationId("corr-" + transaction.getTransactionId())
                .build();

        kafkaProducerService.sendMessage("transaction-validated", event);
    }

    private void publishTransactionValidationFailedEvent(Transaction transaction, List<String> riskFlags) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("evt-" + transaction.getTransactionId() + "-2")
                .transactionId(transaction.getTransactionId())
                .customerId(transaction.getCustomerId())
                .amount(transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0)
                .currency(transaction.getCurrency())
                .merchant(transaction.getMerchant())
                .merchantCategory(transaction.getMerchantCategory())
                .timestamp(transaction.getTimestamp())
                .channel(transaction.getChannel())
                .device(transaction.getDevice() != null ? transaction.getDevice().getType() : null)
                .location(transaction.getLocation())
                .eventTypeString("TransactionValidationFailed")
                .eventTimestamp(Instant.now())
                .correlationId("corr-" + transaction.getTransactionId())
                .rejectionReason(String.join(",", riskFlags))
                .build();

        kafkaProducerService.sendMessage("transaction-validation-failed", event);
    }

}