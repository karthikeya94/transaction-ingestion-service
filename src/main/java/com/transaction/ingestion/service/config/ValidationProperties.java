package com.transaction.ingestion.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "validation")
@Data
public class ValidationProperties {
    private List<String> devices;
    private List<String> channels;
    private CurrencyConfig currency;
    private AmountConfig amount;
    private TimestampConfig timestamp;
    private CustomerConfig customer;
    private ValidationRules rules;

    @Data
    public static class CurrencyConfig {
        private List<String> isoCodes;
        private boolean strictIsoValidation;
    }

    @Data
    public static class AmountConfig {
        private Double minimumAmount;
        private Double maximumAmount;
        private Map<String, Double> tierLimits;
    }

    @Data
    public static class TimestampConfig {
        private Integer timeWindowMinutes;
        private boolean allowFutureDates;
    }

    @Data
    public static class CustomerConfig {
        private String idPattern;
        private List<String> validStatuses;
    }

    @Data
    public static class ValidationRules {
        private boolean enableDuplicateCheck;
        private Integer duplicateWindowSeconds;
        private boolean enableVelocityCheck;
        private Integer maxTransactionsPerHour;
        private boolean enableKycCheck;
        private Integer kycExpiryDays;
    }
}
