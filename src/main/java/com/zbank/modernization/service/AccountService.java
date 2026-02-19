package com.zbank.modernization.service;

import com.zbank.modernization.dto.AccountDTO;
import com.zbank.modernization.dto.AccountInquiryRequest;
import com.zbank.modernization.dto.AccountInquiryResponse;
import com.zbank.modernization.entity.Account;
import com.zbank.modernization.exception.AccountNotFoundException;
import com.zbank.modernization.exception.ValidationException;
import com.zbank.modernization.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Account Service Module - Core business logic for account operations
 * Traceability: WF-001 (Account Inquiry), WF-002 (Balance Management)
 * BDD Coverage: F-001, F-002, F-004
 * Capability: L2-CAP-001 (Account Inquiry & Lookup)
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.US);

    private final AccountRepository accountRepository;
    private final ValidationService validationService;

    public AccountService(AccountRepository accountRepository, ValidationService validationService) {
        this.accountRepository = accountRepository;
        this.validationService = validationService;
    }

    /**
     * Retrieves account information by account number
     * Requirements: WF-001 steps 2-4, BR-001 (account number validation)
     * BDD: F-001 Scenario 1 - Valid account inquiry
     */
    @Cacheable(value = "accounts", key = "#request.accountNumber")
    public AccountInquiryResponse inquireAccount(AccountInquiryRequest request) {
        logger.info("Processing account inquiry for account: {}", request.getAccountNumber());

        // Step 1: Validate account number format (WF-001 Step 2)
        try {
            validationService.validateAccountNumber(request.getAccountNumber());
        } catch (ValidationException e) {
            logger.warn("Invalid account number format: {}", request.getAccountNumber());
            throw e;
        }

        // Step 2: Retrieve account record (WF-001 Step 3)
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", request.getAccountNumber());
                    return new AccountNotFoundException(
                            String.format("Account %s not found", request.getAccountNumber())
                    );
                });

        // Step 3: Build response with account details (WF-001 Step 4)
        AccountDTO accountDTO = mapToDTO(account);
        
        logger.info("Account inquiry successful for: {}", request.getAccountNumber());
        return AccountInquiryResponse.builder()
                .success(true)
                .account(accountDTO)
                .message("Account retrieved successfully")
                .build();
    }

    /**
     * Retrieves current balance with currency formatting
     * Requirements: WF-002, BR-002 (balance precision)
     * BDD: F-002 Scenario 1 - Balance retrieval with formatting
     */
    public String getFormattedBalance(String accountNumber) {
        logger.debug("Retrieving formatted balance for account: {}", accountNumber);
        
        validationService.validateAccountNumber(accountNumber);
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account %s not found", accountNumber)
                ));

        BigDecimal balance = account.getCurrentBalance();
        String formatted = CURRENCY_FORMATTER.format(balance);
        
        logger.debug("Formatted balance for {}: {}", accountNumber, formatted);
        return formatted;
    }

    /**
     * Retrieves raw balance for internal processing
     * Requirements: WF-002 Step 2
     * Capability: L2-CAP-002 (Balance Management)
     */
    public BigDecimal getCurrentBalance(String accountNumber) {
        validationService.validateAccountNumber(accountNumber);
        
        return accountRepository.findByAccountNumber(accountNumber)
                .map(Account::getCurrentBalance)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account %s not found", accountNumber)
                ));
    }

    /**
     * Maps Account entity to DTO with all customer information
     * Requirements: ENT-001, ENT-002 (Customer entity)
     * BDD: F-004 - Customer information display
     */
    private AccountDTO mapToDTO(Account account) {
        return AccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .customerName(account.getCustomerName())
                .customerAddress(account.getCustomerAddress())
                .customerCity(account.getCustomerCity())
                .customerState(account.getCustomerState())
                .customerZipCode(account.getCustomerZipCode())
                .customerPhone(account.getCustomerPhone())
                .accountType(account.getAccountType())
                .currentBalance(account.getCurrentBalance())
                .formattedBalance(CURRENCY_FORMATTER.format(account.getCurrentBalance()))
                .openDate(account.getOpenDate())
                .lastActivityDate(account.getLastActivityDate())
                .status(account.getStatus())
                .build();
    }

    /**
     * Health check for account service availability
     * Used by readiness probes
     */
    public boolean isHealthy() {
        try {
            accountRepository.count();
            return true;
        } catch (Exception e) {
            logger.error("Account service health check failed", e);
            return false;
        }
    }
}