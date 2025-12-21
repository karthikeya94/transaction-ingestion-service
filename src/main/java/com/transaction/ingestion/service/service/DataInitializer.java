package com.transaction.ingestion.service.service;

import com.riskplatform.common.entity.Customer;
import com.riskplatform.common.entity.Transaction;
import com.riskplatform.common.model.Location;
import com.transaction.ingestion.service.config.DataInitializationConfig;
import com.transaction.ingestion.service.repository.CustomerRepository;
import com.transaction.ingestion.service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

//@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    // private final DataInitializationConfig config;

    // @Override
    public void run() throws Exception {
        if (false) {
            log.info("Data initialization is disabled. Skipping dummy data creation.");
            return;
        }

        log.info("Initializing dummy data...");

        // Clear existing data
        // customerRepository.deleteAll();
        // transactionRepository.deleteAll();

        // Create dummy customers
        List<Customer> customers = createDummyCustomers();
        customerRepository.saveAll(customers);
        log.info("Created {} dummy customers", customers.size());

        // Create dummy transactions
        List<Transaction> transactions = createDummyTransactions(customers);
        transactionRepository.saveAll(transactions);
        log.info("Created {} dummy transactions", transactions.size());

        log.info("Dummy data initialization completed.");
    }

    private Customer createCustomer(String customerId, String name, String email, String status, String tier,
            Double dailyLimit, Double transactionLimit, Boolean blacklisted) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setName(name);
        customer.setEmail(email);
        customer.setStatus(status);
        customer.setTier(tier);
        customer.setDailyLimit(dailyLimit);
        customer.setTransactionLimit(transactionLimit);
        customer.setBlacklisted(blacklisted);
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        return customer;
    }

    private Transaction createTransactionObject(String transactionId, String customerId, Double amount, String currency,
            String merchant, String merchantCategory, Instant timestamp, String channel,
            String device, Location location, String status, Instant createdAt, Instant updatedAt) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setCustomerId(customerId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setMerchant(merchant);
        transaction.setMerchantCategory(merchantCategory);
        transaction.setTimestamp(timestamp);
        transaction.setChannel(channel);
        transaction.setDevice(device);
        transaction.setLocation(location);
        transaction.setStatus(status);
        transaction.setCreatedAt(createdAt);
        transaction.setUpdatedAt(updatedAt);
        return transaction;
    }

    private List<Customer> createDummyCustomers() {
        return Arrays.asList(
                createCustomer("C100001", "John Smith", "john.smith@example.com", "ACTIVE", "PREMIUM", 50000.00,
                        10000.00, false),
                createCustomer("C100002", "Jane Doe", "jane.doe@example.com", "ACTIVE", "STANDARD", 10000.00, 5000.00,
                        false),
                createCustomer("C100003", "Robert Johnson", "robert.johnson@example.com", "ACTIVE", "BASIC", 5000.00,
                        1000.00, false),
                createCustomer("C100004", "Emily Williams", "emily.williams@example.com", "INACTIVE", "STANDARD",
                        10000.00, 2000.00, false),
                createCustomer("C100005", "Michael Brown", "michael.brown@example.com", "ACTIVE", "PREMIUM", 50000.00,
                        15000.00, true));
    }

    private List<Transaction> createDummyTransactions(List<Customer> customers) {
        Random random = new Random();
        String[] merchants = { "Amazon", "Walmart", "Target", "Best Buy", "Starbucks", "McDonald's", "Subway", "Shell",
                "Exxon", "Costco" };
        String[] merchantCategories = { "E-commerce", "Retail", "Food & Beverage", "Gas Station", "Wholesale" };
        String[] channels = { "online", "atm", "branch", "mobile" };
        String[] devices = { "desktop", "mobile", "tablet", "kiosk" };
        String[] currencies = { "USD", "EUR", "GBP", "JPY", "INR" };
        String[] countries = { "USA", "UK", "Germany", "Japan", "India" };
        String[] cities = { "New York", "London", "Berlin", "Tokyo", "Mumbai" };

        return Arrays.asList(
                createTransaction(customers.get(0), merchants[0], merchantCategories[0], channels[0], devices[1],
                        currencies[0], countries[0], cities[0], random),
                createTransaction(customers.get(0), merchants[1], merchantCategories[1], channels[3], devices[1],
                        currencies[0], countries[0], cities[0], random),
                createTransaction(customers.get(1), merchants[4], merchantCategories[2], channels[2], devices[0],
                        currencies[0], countries[0], cities[1], random),
                createTransaction(customers.get(1), merchants[5], merchantCategories[2], channels[1], devices[2],
                        currencies[0], countries[0], cities[1], random),
                createTransaction(customers.get(2), merchants[8], merchantCategories[3], channels[0], devices[1],
                        currencies[0], countries[0], cities[2], random),
                createTransaction(customers.get(3), merchants[2], merchantCategories[1], channels[3], devices[3],
                        currencies[0], countries[0], cities[3], random),
                createTransaction(customers.get(4), merchants[9], merchantCategories[4], channels[0], devices[0],
                        currencies[0], countries[0], cities[4], random));
    }

    private Transaction createTransaction(Customer customer, String merchant, String merchantCategory, String channel,
            String device, String currency, String country, String city, Random random) {
        // Generate random amount based on customer tier and limits
        double maxAmount = customer.getTransactionLimit() != null ? customer.getTransactionLimit() : 1000.00;
        double amount = ThreadLocalRandom.current().nextDouble(10.00, Math.min(maxAmount, 5000.00));

        // Generate random timestamp within last 30 days
        Instant now = Instant.now();
        Instant randomTimestamp = now.minus(ThreadLocalRandom.current().nextLong(30), ChronoUnit.DAYS);

        // Create location
        Location location = new Location();
        location.setCountry(country);
        location.setCity(city);
        location.setIp("192.168." + random.nextInt(255) + "." + random.nextInt(255));

        return createTransactionObject("T" + System.currentTimeMillis() + "-" + random.nextInt(1000),
                customer.getCustomerId(), Math.round(amount * 100.0) / 100.0, currency, merchant,
                merchantCategory, randomTimestamp, channel, device, location, "COMPLETED", now, now);
    }
}