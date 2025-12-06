package com.transaction.ingestion.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Detail {
    private String field;
    private String message;
}