package com.transaction.ingestion.service.controller;

import com.transaction.ingestion.service.model.*;
import com.transaction.ingestion.service.dto.*;
import com.transaction.ingestion.service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@AllArgsConstructor
public class TransactionsController {

    private final TransactionService transactionService;

    @Operation(summary = "Ingest a new transaction", description = "Process and validate a new transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction accepted", content = @Content(schema = @Schema(implementation = IngestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transaction rejected", content = @Content(schema = @Schema(implementation = RejectedResponse.class)))
    })
    @PostMapping("/ingest")
    public ResponseEntity<?> processIngest(@RequestBody IngestRequest ingestRequest) {
        return transactionService.processTransaction(ingestRequest);
    }

    @Operation(summary = "Get transaction by ID", description = "Retrieve a transaction by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = Transaction.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String transactionId) {
        return transactionService.findTransactionById(transactionId);
    }
}
