package com.transaction.ingestion.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Error {
    private List<Detail> details;
}