package com.transaction.ingestion.service;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@OpenAPIDefinition(info = @Info(title = "Transaction Ingestion API", version = "1.0", description = "API for ingesting transactions"))
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
public class TransactionIngestionServiceApplication {

    public static void main(String[] args) {
		SpringApplication.run(TransactionIngestionServiceApplication.class, args);
	}
}
