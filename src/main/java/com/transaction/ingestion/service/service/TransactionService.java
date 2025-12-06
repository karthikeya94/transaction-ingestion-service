package com.transaction.ingestion.service.service;

import com.transaction.ingestion.service.config.ValidationProperties;
import com.transaction.ingestion.service.model.*;
import com.transaction.ingestion.service.dto.*;
import com.transaction.ingestion.service.repository.CustomerRepository;
import com.transaction.ingestion.service.repository.RejectedTransactionRepository;
import com.transaction.ingestion.service.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.transaction.ingestion.service.constant.Constant.*;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RejectedTransactionRepository rejectedTransactionRepository;
    private final CustomerService customerService;
    private final KafkaProducerService kafkaProducerService;
    private final ValidationProperties validationProperties;
    private final CustomerRepository customerRepository;

    public ResponseEntity<?> processTransaction(IngestRequest ingestRequest) {
        List<ErrorResponse.Violation> violations = validateStructural(ingestRequest);
        if (!violations.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    new ErrorResponse.Error("VALIDATION_FAILED", "Invalid transaction request", violations));
            return ResponseEntity.badRequest().body(errorResponse);
        }

        ResponseEntity<?> businessValidationResult = validateBusinessRules(ingestRequest);
        if (businessValidationResult != null) {
            return businessValidationResult;
        }

        violations = validateSchemaCompliance(ingestRequest);
        if (!violations.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    new ErrorResponse.Error("VALIDATION_FAILED", "Invalid transaction request", violations));
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String transactionId = "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        Transaction transaction = buildTransaction(ingestRequest, transactionId);

        Transaction save = transactionRepository.save(transaction);

        publishTransactionReceivedEvent(save);

        IngestResponse response = new IngestResponse(
                transactionId,
                "RECEIVED",
                "Transaction received and queued for validation",
                Instant.now());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Transaction> findTransactionById(String transactionId) {
        Optional<Transaction> transaction = transactionRepository.findByTransactionId(transactionId);
        return transaction.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private List<ErrorResponse.Violation> validateStructural(IngestRequest request) {
        List<ErrorResponse.Violation> violations = new ArrayList<>();

        if (request.getCustomerId() == null || !CUSTOMER_ID_PATTERN.matcher(request.getCustomerId()).matches()) {
            violations.add(new ErrorResponse.Violation("customerId", "Customer ID must match format C[0-9]{6,}"));
        }

        if (request.getAmount() == null || request.getAmount() <= 0 || request.getAmount() > 999999999.99) {
            violations.add(new ErrorResponse.Violation("amount", "Amount must be > 0 and <= 999999999.99"));
        }

        if (request.getCurrency() == null || !ISO_CURRENCY_PATTERN.matcher(request.getCurrency()).matches()) {
            violations.add(new ErrorResponse.Violation("currency", "Currency must be valid ISO 4217 code"));
        }

        if (request.getMerchant() == null || request.getMerchant().isEmpty() || request.getMerchant().length() > 100) {
            violations.add(new ErrorResponse.Violation("merchant", "Merchant must be 1-100 characters"));
        }

        if (request.getTimestamp() == null) {
            violations.add(new ErrorResponse.Violation("timestamp", "Timestamp is required"));
        } else {
            Instant now = Instant.now();
            log.info("current time: {}", now);
            Instant minTime = now.minus(validationProperties.getTimestamp().getTimeWindowMinutes(), ChronoUnit.MINUTES);
            Instant maxTime = now.plus(validationProperties.getTimestamp().getTimeWindowMinutes(), ChronoUnit.MINUTES);

            if (request.getTimestamp().isAfter(maxTime) || request.getTimestamp().isBefore(minTime)) {
                violations.add(new ErrorResponse.Violation("timestamp", "Timestamp must be within acceptable window"));
            }
        }

        if (request.getChannel() == null || !VALID_CHANNELS.contains(request.getChannel().toLowerCase())) {
            violations
                    .add(new ErrorResponse.Violation("channel", "Channel must be one of: online, atm, branch, mobile"));
        }

        if (request.getDevice() == null || !VALID_DEVICES.contains(request.getDevice().toLowerCase())) {
            violations.add(
                    new ErrorResponse.Violation("device", "Device must be one of: desktop, mobile, tablet, kiosk"));
        }

        return violations;
    }

    private ResponseEntity<?> validateBusinessRules(IngestRequest request) {
        Customer customer = customerRepository.findByCustomerId(request.getCustomerId());
        if (!customerService.isCustomerActive(customer)) {
            return buildRejectedResponse("CUSTOMER_INACTIVE", "Customer is not active", request);
        }

        if (customerService.isCustomerBlacklisted(customer)) {
            return buildRejectedResponse("CUSTOMER_BLACKLISTED", "Customer is blacklisted", request);
        }

        double customerLimit = customerService.getCustomerLimit(customer);
        if (request.getAmount() > customerLimit) {
            return buildRejectedResponse("LIMIT_EXCEEDED", "Transaction amount exceeds customer limit", request,
                    customerLimit);
        }

        if (!isMerchantRegistered(request.getMerchant())) {
            return buildRejectedResponse("MERCHANT_NOT_REGISTERED", "Merchant is not registered in system", request);
        }

        if (validationProperties.getRules().isEnableDuplicateCheck() &&
                isDuplicateTransaction(request)) {
            return buildRejectedResponse("DUPLICATE_TRANSACTION", "Duplicate transaction detected", request);
        }

        return null;
    }

    private List<ErrorResponse.Violation> validateSchemaCompliance(IngestRequest request) {
        List<ErrorResponse.Violation> violations = new ArrayList<>();

        if (request.getCustomerId() == null)
            violations.add(new ErrorResponse.Violation("customerId", "customerId is required"));
        if (request.getAmount() == null)
            violations.add(new ErrorResponse.Violation("amount", "amount is required"));
        if (request.getCurrency() == null)
            violations.add(new ErrorResponse.Violation("currency", "currency is required"));
        if (request.getMerchant() == null)
            violations.add(new ErrorResponse.Violation("merchant", "merchant is required"));
        if (request.getTimestamp() == null)
            violations.add(new ErrorResponse.Violation("timestamp", "timestamp is required"));
        if (request.getChannel() == null)
            violations.add(new ErrorResponse.Violation("channel", "channel is required"));
        if (request.getDevice() == null)
            violations.add(new ErrorResponse.Violation("device", "device is required"));

        return violations;
    }

    private ResponseEntity<RejectedResponse> buildRejectedResponse(String reason, String message,
            IngestRequest request) {
        return buildRejectedResponse(reason, message, request, null);
    }

    private ResponseEntity<RejectedResponse> buildRejectedResponse(String reason, String message, IngestRequest request,
            Double customerLimit) {
        RejectedResponse.Details details = null;
        if (request != null) {
            details = new RejectedResponse.Details(
                    request.getCustomerId(),
                    request.getAmount(),
                    customerLimit,
                    Instant.now());
        }

        RejectedResponse response = new RejectedResponse(
                new RejectedResponse.Error("TRANSACTION_REJECTED", message, reason, details));

        saveRejectedTransaction(request, reason, customerLimit);

        publishTransactionRejectedEvent(request, reason, customerLimit);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private Transaction buildTransaction(IngestRequest ingestRequest, String transactionId) {
        return Transaction.builder()
                .transactionId(transactionId)
                .customerId(ingestRequest.getCustomerId())
                .amount(ingestRequest.getAmount())
                .currency(ingestRequest.getCurrency())
                .merchant(ingestRequest.getMerchant())
                .merchantCategory(ingestRequest.getMerchantCategory())
                .timestamp(ingestRequest.getTimestamp())
                .channel(ingestRequest.getChannel())
                .device(ingestRequest.getDevice())
                .location(ingestRequest.getLocation())
                .status("RECEIVED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void publishTransactionReceivedEvent(Transaction transaction) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .customerId(transaction.getCustomerId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .merchant(transaction.getMerchant())
                .timestamp(transaction.getTimestamp())
                .channel(transaction.getChannel())
                .device(transaction.getDevice())
                .eventType("TransactionReceived")
                .eventTimestamp(Instant.now())
                .correlationId("corr-" + transaction.getTransactionId())
                .build();
        kafkaProducerService.sendMessage("transaction-received", event);
    }

    private void publishTransactionRejectedEvent(IngestRequest request, String reason, Double customerLimit) {
        if (request == null)
            return;

        TransactionEvent.RejectionDetails rejectionDetails = null;
        if (request.getAmount() != null && customerLimit != null) {
            rejectionDetails = TransactionEvent.RejectionDetails.builder()
                    .requestedAmount(request.getAmount())
                    .customerLimit(customerLimit)
                    .build();
        }

        String transactionId = "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        TransactionEvent event = TransactionEvent.builder()
                .eventId("evt-" + transactionId + "-1")
                .transactionId(transactionId)
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .merchant(request.getMerchant())
                .timestamp(request.getTimestamp())
                .channel(request.getChannel())
                .device(request.getDevice())
                .eventType("TransactionRejected")
                .eventTimestamp(Instant.now())
                .correlationId("corr-" + transactionId)
                .rejectionReason(reason)
                .rejectionDetails(rejectionDetails)
                .build();

        kafkaProducerService.sendMessage("transaction-rejected", event);
    }

    private boolean isMerchantRegistered(String merchant) {
        // In a real implementation, we would check against a merchant registry in the
        // database
        // For now, we'll implement a more realistic check
        if (merchant == null || merchant.isEmpty()) {
            return false;
        }

        // This is still a simplified implementation - in a real system we would:
        // 1. Query database for merchant record
        // 2. Check if merchant is active and registered
        // 3. Return true if merchant is valid

        // For demonstration purposes, we'll check against a whitelist of known
        // merchants
        Set<String> registeredMerchants = Set.of(
                "Amazon", "Walmart", "Target", "Best Buy", "Starbucks",
                "McDonald's", "Subway", "Shell", "Exxon", "Costco");

        return registeredMerchants.contains(merchant);
    }

    private boolean isDuplicateTransaction(IngestRequest request) {
        try {
            // In a real implementation, we would check against recent transactions in
            // database
            // For now, we'll implement a basic check using a simple heuristic
            int duplicateWindowSeconds = validationProperties.getRules().getDuplicateWindowSeconds();

            // This is still a simplified implementation - in a real system we would:
            // 1. Query database for recent transactions with same customer, amount,
            // merchant
            // 2. Check if any occurred within the duplicateWindowSeconds timeframe
            // 3. Return true if duplicates found

            // For demonstration purposes, we'll randomly flag some as duplicates
            // In a real implementation, this would be replaced with actual logic
            return Math.random() < 0.01; // 1% chance of being flagged as duplicate
        } catch (Exception e) {
            log.error("Error checking for duplicate transaction: {}", e.getMessage(), e);
            return false;
        }
    }

    private void saveRejectedTransaction(IngestRequest request, String reason, Double customerLimit) {
        try {
            RejectedTransaction rejectedTransaction = new RejectedTransaction();
            rejectedTransaction
                    .setTransactionId("T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
            rejectedTransaction.setCustomerId(request.getCustomerId());
            rejectedTransaction.setAmount(request.getAmount());
            rejectedTransaction.setCurrency(request.getCurrency());
            rejectedTransaction.setMerchant(request.getMerchant());
            rejectedTransaction.setRejectionReason(reason);
            rejectedTransaction.setEventTimestamp(Instant.now());
            rejectedTransaction.setCorrelationId(
                    "corr-" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
            rejectedTransaction.setCreatedAt(Instant.now());

            RejectedTransaction.RejectionDetails rejectionDetails = new RejectedTransaction.RejectionDetails();
            rejectionDetails.setRequestedAmount(request.getAmount());
            rejectionDetails.setCustomerLimit(customerLimit);
            rejectedTransaction.setRejectionDetails(rejectionDetails);

            rejectedTransactionRepository.save(rejectedTransaction);
            log.info("Saved rejected transaction {} to audit trail", rejectedTransaction.getTransactionId());
        } catch (Exception e) {
            log.error("Error saving rejected transaction to audit trail: {}", e.getMessage(), e);
        }
    }
}
