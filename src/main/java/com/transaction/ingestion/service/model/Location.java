package com.transaction.ingestion.service.model;

import lombok.Data;

@Data
public class Location {
    private String country;
    private String city;
    private String ip;
}
