package com.transaction.ingestion.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@ConfigurationProperties(prefix = "app.data-initialization")
public class DataInitializationConfig {
    
    private boolean enabled = false;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}