package com.zbank.modernization.controller;

import com.zbank.modernization.dto.TransactionRequest;
import com.zbank.modernization.dto.TransactionResponse;
import com.zbank.modernization.dto.TransactionHistoryResponse;
import com.zbank.modernization.exception.AccountNotFoundException;
import com.zbank.modernization.exception.InsufficientFundsException;
import com.zbank.modernization.exception.InvalidTransactionException;
import com.zbank.modernization.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API Controller for Transaction Operations
 * 
 * Migrated from: CICS.COB_ZBANK3_.cbl (Transaction processing logic)
 * Implements: WF-002 (Deposit Transaction), WF-003 (Withdrawal Transaction)
 * Traceability: BR-002 (Balance validation), BR-003 (Transaction limits)
 * 
 * @author zBank Modernization Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@Tag(name = "Transaction Management", description = "APIs for transaction processing and history retrieval")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Process a deposit transaction
     * 
     * Legacy Mapping: PERFORM DEPOSIT-TRANSACTION in ZBANK3
     * Workflow: WF-002 (Deposit Transaction)
     * Business Rule: BR-002 (Balance validation after deposit)
     * 
     * @param request Deposit transaction details
     * @return Transaction confirmation with updated balance
     */
    @PostMapping("/deposit")
    @Operation(summary = "Process deposit transaction", 
               description = "Deposits funds into specified account and returns transaction confirmation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Deposit processed successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction request"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "422", description = "Transaction validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> processDeposit(
            @Valid @RequestBody TransactionRequest request) {
        
        logger.info("Processing deposit for account: {}, amount: {}", 
                   request.getAccountNumber(), request.getAmount());
        
        try {
            TransactionResponse response = transactionService.processDeposit(
                request.getAccountNumber(),
                request.getAmount(),
                request.getDescription()
            );
            
            logger.info("Deposit successful - Transaction ID: {}, New Balance: {}",
                       response.getTransactionId(), response.getNewBalance());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (AccountNotFoundException e) {
            logger.warn("Deposit failed - Account not found: {}", request.getAccountNumber());
            throw e;
        } catch (InvalidTransactionException e) {
            logger.warn("Deposit failed - Invalid transaction: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing deposit for account: {}", 
                        request.getAccountNumber(), e);
            throw new RuntimeException("Failed to process deposit transaction", e);
        }
    }

    /**
     * Process a withdrawal transaction
     * 
     * Legacy Mapping: PERFORM WITHDRAWAL-TRANSACTION in ZBANK3
     * Workflow: WF-003 (Withdrawal Transaction)
     * Business Rule: BR-002 (Sufficient balance validation), BR-003 (Transaction limits)
     * 
     * @param request Withdrawal transaction details
     * @return Transaction confirmation with updated balance
     */
    @PostMapping("/withdraw")
    @Operation(summary = "Process withdrawal transaction",
               description = "Withdraws funds from specified account with balance validation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Withdrawal processed successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction request"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "422", description = "Insufficient funds or validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> processWithdrawal(
            @Valid @RequestBody TransactionRequest request) {
        
        logger.info("Processing withdrawal for account: {}, amount: {}",
                   request.getAccountNumber(), request.getAmount());
        
        try {
            TransactionResponse response = transactionService.processWithdrawal(
                request.getAccountNumber(),
                request.getAmount(),
                request.getDescription()
            );
            
            logger.info("Withdrawal successful - Transaction ID: {}, New Balance: {}",
                       response.getTransactionId(), response.getNewBalance());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (AccountNotFoundException e) {
            logger.warn("Withdrawal failed - Account not found: {}", request.getAccountNumber());
            throw e;
        } catch (InsufficientFundsException e) {
            logger.warn("Withdrawal failed - Insufficient funds: {}", e.getMessage());
            throw e;
        } catch (InvalidTransactionException e) {
            logger.warn("Withdrawal failed - Invalid transaction: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing withdrawal for account: {}",
                        request.getAccountNumber(), e);
            throw new RuntimeException("Failed to process withdrawal transaction", e);
        }
    }

    /**
     * Retrieve transaction history for an account
     * 
     * Legacy Mapping: VSAM transaction log retrieval functionality
     * Workflow: WF-001 extension (Account inquiry with transaction history)
     * 
     * @param accountNumber Account identifier
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param limit Maximum number of transactions to return
     * @return List of transactions for the account
     */
    @GetMapping("/history/{accountNumber}")
    @Operation(summary = "Retrieve transaction history",
               description = "Returns transaction history for specified account with optional date filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionHistoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @Parameter(description = "Account number", required = true)
            @PathVariable @NotBlank String accountNumber,
            
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Maximum number of transactions")
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        
        logger.info("Retrieving transaction history for account: {}, startDate: {}, endDate: {}, limit: {}",
                   accountNumber, startDate, endDate, limit);
        
        try {
            TransactionHistoryResponse response = transactionService.getTransactionHistory(
                accountNumber, startDate, endDate, limit
            );
            
            logger.info("Retrieved {} transactions for account: {}",
                       response.getTransactions().size(), accountNumber);
            
            return ResponseEntity.ok(response);
            
        } catch (AccountNotFoundException e) {
            logger.warn("Transaction history retrieval failed - Account not found: {}", accountNumber);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error retrieving transaction history for account: {}",
                        accountNumber, e);
            throw new RuntimeException("Failed to retrieve transaction history", e);
        }
    }

    /**
     * Retrieve specific transaction details
     * 
     * @param transactionId Unique transaction identifier
     * @return Transaction details
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Retrieve transaction details",
               description = "Returns details of a specific transaction by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable @NotBlank String transactionId) {
        
        logger.info("Retrieving transaction details for ID: {}", transactionId);
        
        try {
            TransactionResponse response = transactionService.getTransactionById(transactionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to retrieve transaction", e);
        }
    }

    /**
     * Health check endpoint for transaction service
     * 
     * @return Service status
     */
    @GetMapping("/health")
    @Operation(summary = "Transaction service health check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Transaction service operational");
    }
}