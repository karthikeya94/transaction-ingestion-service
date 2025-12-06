package com.transaction.ingestion.service.constant;

import java.util.Set;
import java.util.regex.Pattern;

public class Constant {
    public static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^C[0-9]{6,}$");
    public static final Pattern ISO_CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
    public static final Set<String> VALID_CHANNELS = Set.of("online", "atm", "branch", "mobile");
    public static final Set<String> VALID_DEVICES = Set.of("desktop", "mobile", "tablet", "kiosk");
    public static final Set<String> SANCTIONED_MERCHANTS = Set.of("BAD_MERCHANT_1", "BAD_MERCHANT_2");
    public static final Set<String> SANCTIONED_COUNTRIES = Set.of("BAD_COUNTRY_1", "BAD_COUNTRY_2");

}
