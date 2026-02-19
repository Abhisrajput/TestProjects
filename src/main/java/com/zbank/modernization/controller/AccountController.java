package com.zbank.modernization.controller;

import com.zbank.modernization.dto.AccountDto;
import com.zbank.modernization.dto.AccountHistoryDto;
import com.zbank.modernization.dto.TransactionDto;
import com.zbank.modernization.dto.ApiResponse;
import com.zbank.modernization.exception.AccountNotFoundException;
import com.zbank.modernization.exception.InvalidAccountException;
import com.zbank.modernization.service.AccountService;
import com.zbank.modernization.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST API Controller for Account Management Operations
 * 
 * Modernization Trace:
 * - Legacy: CICS.COB_ZBANK3_.cbl (Account inquiry and management transactions)
 * - Requirement: WF-001 (Account Inquiry), WF-003 (Account History)
 * - Architecture: RESTful API layer replacing CICS terminal interface
 * 
 * Endpoints expose core banking functions:
 * - Account inquiry by account number
 * - Account history retrieval
 * - Balance inquiry
 * - Account status management
 * 
 * @author zBank Modernization Team
 * @version 1.0
 * @since 2024-01
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@Tag(name = "Account Management", description = "Account inquiry and management endpoints")
@CrossOrigin(origins = "${zbank.cors.allowed-origins}", maxAge = 3600)
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private static final String ACCOUNT_NUMBER_PATTERN = "^[0-9]{10}$";

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    /**
     * Retrieve account details by account number
     * 
     * Legacy Mapping: CICS transaction code 'ACCT' - Account inquiry function
     * Requirement: WF-001 (Account Inquiry)
     * 
     * @param accountNumber 10-digit account identifier
     * @return Account details including balance, status, customer info
     */
    @Operation(
        summary = "Get account details",
        description = "Retrieve comprehensive account information by account number. Maps to legacy CICS ACCT transaction."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Account found and returned",
            content = @Content(schema = @Schema(implementation = AccountDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid account number format",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('TELLER', 'CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(
            @Parameter(description = "10-digit account number", example = "1234567890")
            @PathVariable
            @NotBlank(message = "Account number is required")
            @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "Account number must be 10 digits")
            String accountNumber) {
        
        logger.info("Account inquiry request received for account: {}", accountNumber);
        
        try {
            AccountDto account = accountService.getAccountByNumber(accountNumber);
            logger.debug("Account found: {} with balance: {}", accountNumber, account.getCurrentBalance());
            
            return ResponseEntity.ok(
                ApiResponse.<AccountDto>builder()
                    .success(true)
                    .data(account)
                    .message("Account retrieved successfully")
                    .build()
            );
        } catch (AccountNotFoundException e) {
            logger.warn("Account not found: {}", accountNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<AccountDto>builder()
                    .success(false)
                    .message("Account not found: " + accountNumber)
                    .errorCode("ACCOUNT_NOT_FOUND")
                    .build());
        }
    }

    /**
     * Get account balance
     * 
     * Legacy Mapping: CICS.COB_ZBANK3_.cbl balance inquiry function
     * Requirement: WF-001 (Account Inquiry - Balance component)
     * 
     * @param accountNumber 10-digit account identifier
     * @return Current account balance
     */
    @Operation(
        summary = "Get account balance",
        description = "Retrieve current balance for specified account"
    )
    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('TELLER', 'CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> getAccountBalance(
            @PathVariable
            @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "Account number must be 10 digits")
            String accountNumber) {
        
        logger.info("Balance inquiry for account: {}", accountNumber);
        
        try {
            BigDecimal balance = accountService.getAccountBalance(accountNumber);
            
            return ResponseEntity.ok(
                ApiResponse.<BigDecimal>builder()
                    .success(true)
                    .data(balance)
                    .message("Balance retrieved successfully")
                    .build()
            );
        } catch (AccountNotFoundException e) {
            logger.warn("Account not found for balance inquiry: {}", accountNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<BigDecimal>builder()
                    .success(false)
                    .message("Account not found")
                    .errorCode("ACCOUNT_NOT_FOUND")
                    .build());
        }
    }

    /**
     * Retrieve account transaction history
     * 
     * Legacy Mapping: CICS.COB_ZBANK3_.cbl transaction history display
     * Requirement: WF-003 (Account History)
     * 
     * @param accountNumber 10-digit account identifier
     * @param limit Maximum number of transactions to return (default: 50)
     * @return List of recent transactions
     */
    @Operation(
        summary = "Get account transaction history",
        description = "Retrieve paginated transaction history for account. Maps to legacy transaction history display."
    )
    @GetMapping("/{accountNumber}/transactions")
    @PreAuthorize("hasAnyRole('TELLER', 'CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AccountHistoryDto>> getAccountHistory(
            @PathVariable
            @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "Account number must be 10 digits")
            String accountNumber,
            
            @Parameter(description = "Maximum number of transactions to return")
            @RequestParam(defaultValue = "50") int limit,
            
            @Parameter(description = "Page offset for pagination")
            @RequestParam(defaultValue = "0") int offset) {
        
        logger.info("Transaction history request for account: {} (limit={}, offset={})", 
                    accountNumber, limit, offset);
        
        try {
            List<TransactionDto> transactions = transactionService
                .getAccountTransactions(accountNumber, limit, offset);
            
            AccountDto account = accountService.getAccountByNumber(accountNumber);
            
            AccountHistoryDto history = AccountHistoryDto.builder()
                .accountNumber(accountNumber)
                .customerName(account.getCustomerName())
                .currentBalance(account.getCurrentBalance())
                .transactions(transactions)
                .totalCount(transactionService.getTransactionCount(accountNumber))
                .build();
            
            return ResponseEntity.ok(
                ApiResponse.<AccountHistoryDto>builder()
                    .success(true)
                    .data(history)
                    .message("Transaction history retrieved successfully")
                    .build()
            );
        } catch (AccountNotFoundException e) {
            logger.warn("Account not found for history request: {}", accountNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<AccountHistoryDto>builder()
                    .success(false)
                    .message("Account not found")
                    .errorCode("ACCOUNT_NOT_FOUND")
                    .build());
        }
    }

    /**
     * Update account status (active/inactive/frozen)
     * 
     * Legacy Mapping: CICS account status management function
     * Requirement: BR-002 (Account Validation - Status management)
     * 
     * @param accountNumber 10-digit account identifier
     * @param status New account status
     * @return Updated account details
     */
    @Operation(
        summary = "Update account status",
        description = "Change account status (ACTIVE, INACTIVE, FROZEN). Requires admin privileges."
    )
    @PatchMapping("/{accountNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccountStatus(
            @PathVariable
            @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "Account number must be 10 digits")
            String accountNumber,
            
            @Parameter(description = "New account status", example = "ACTIVE")
            @RequestParam
            @Pattern(regexp = "^(ACTIVE|INACTIVE|FROZEN)$", message = "Status must be ACTIVE, INACTIVE, or FROZEN")
            String status) {
        
        logger.info("Account status update request: {} -> {}", accountNumber, status);
        
        try {
            AccountDto updatedAccount = accountService.updateAccountStatus(accountNumber, status);
            
            logger.info("Account status updated successfully: {} -> {}", accountNumber, status);
            
            return ResponseEntity.ok(
                ApiResponse.<AccountDto>builder()
                    .success(true)
                    .data(updatedAccount)
                    .message("Account status updated successfully")
                    .build()
            );
        } catch (AccountNotFoundException e) {
            logger.warn("Account not found for status update: {}", accountNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<AccountDto>builder()
                    .success(false)
                    .message("Account not found")
                    .errorCode("ACCOUNT_NOT_FOUND")
                    .build());
        } catch (InvalidAccountException e) {
            logger.error("Invalid status transition for account: {}", accountNumber, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<AccountDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .errorCode("INVALID_STATUS_TRANSITION")
                    .build());
        }
    }

    /**
     * Search accounts by customer name
     * 
     * Requirement: Support for customer service operations
     * 
     * @param customerName Partial or full customer name
     * @return List of matching accounts
     */
    @Operation(
        summary = "Search accounts by customer name",
        description = "Find accounts matching customer name pattern. Returns partial matches."
    )
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountDto>>> searchAccounts(
            @Parameter(description = "Customer name search term")
            @RequestParam
            @NotBlank(message = "Search term is required")
            String customerName) {
        
        logger.info("Account search request for customer: {}", customerName);
        
        List<AccountDto> accounts = accountService.searchAccountsByCustomerName(customerName);
        
        return ResponseEntity.ok(
            ApiResponse.<List<AccountDto>>builder()
                .success(true)
                .data(accounts)
                .message(accounts.size() + " account(s) found")
                .build()
        );
    }

    /**
     * Validate account exists and is active
     * 
     * Legacy Mapping: Account validation logic from COBOL programs
     * Requirement: BR-002 (Account Validation)
     * 
     * @param accountNumber 10-digit account identifier
     * @return Validation result
     */
    @Operation(
        summary = "Validate account",
        description = "Check if account exists and is in active status"
    )
    @GetMapping("/{accountNumber}/validate")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<Boolean>> validateAccount(
            @PathVariable
            @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "Account number must be 10 digits")
            String accountNumber) {
        
        logger.debug("Account validation request: {}", accountNumber);
        
        boolean isValid = accountService.isAccountValid(accountNumber);
        
        return ResponseEntity.ok(
            ApiResponse.<Boolean>builder()
                .success(true)
                .data(isValid)
                .message(isValid ? "Account is valid" : "Account is invalid or inactive")
                .build()
        );
    }
}