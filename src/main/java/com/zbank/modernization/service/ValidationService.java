package com.zbank.modernization.service;

import com.zbank.modernization.dto.TransactionRequest;
import com.zbank.modernization.entity.Account;
import com.zbank.modernization.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ValidationService - Business rule validation engine
 * Traceability:
 * - BR-001: Account number format validation (8 digits)
 * - BR-002: Transaction amount validation (0.01 to 999999.99)
 * - BR-003: Balance sufficiency checks
 * - BR-004: Daily transaction limits
 */
@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{8}$");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("999999.99");
    private static final BigDecimal DAILY_TRANSACTION_LIMIT = new BigDecimal("50000.00");
    private static final int MAX_DAILY_TRANSACTIONS = 100;

    /**
     * Validates account number format per BR-001
     * @param accountNumber Account number to validate
     * @throws ValidationException if format invalid
     */
    public void validateAccountNumber(String accountNumber) {
        logger.debug("Validating account number: {}", accountNumber);
        
        List<String> errors = new ArrayList<>();
        
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            errors.add("Account number is required");
        } else if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            errors.add("Account number must be exactly 8 digits");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("BR-001", "Account number validation failed", errors);
        }
    }

    /**
     * Validates transaction amount per BR-002
     * @param amount Transaction amount
     * @throws ValidationException if amount invalid
     */
    public void validateTransactionAmount(BigDecimal amount) {
        logger.debug("Validating transaction amount: {}", amount);
        
        List<String> errors = new ArrayList<>();
        
        if (amount == null) {
            errors.add("Transaction amount is required");
        } else {
            if (amount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
                errors.add(String.format("Transaction amount must be at least %s", MIN_TRANSACTION_AMOUNT));
            }
            if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
                errors.add(String.format("Transaction amount cannot exceed %s", MAX_TRANSACTION_AMOUNT));
            }
            if (amount.scale() > 2) {
                errors.add("Transaction amount cannot have more than 2 decimal places");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("BR-002", "Transaction amount validation failed", errors);
        }
    }

    /**
     * Validates account balance sufficiency per BR-003
     * @param account Account to validate
     * @param transactionAmount Proposed transaction amount
     * @throws ValidationException if insufficient funds
     */
    public void validateBalanceSufficiency(Account account, BigDecimal transactionAmount) {
        logger.debug("Validating balance sufficiency for account {}: balance={}, transaction={}", 
                     account.getAccountNumber(), account.getCurrentBalance(), transactionAmount);
        
        if (transactionAmount.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal newBalance = account.getCurrentBalance().add(transactionAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException(
                    "BR-003",
                    "Insufficient funds",
                    List.of(String.format("Current balance %s insufficient for transaction amount %s",
                                          account.getCurrentBalance(), transactionAmount.abs()))
                );
            }
        }
    }

    /**
     * Validates daily transaction limits per BR-004
     * @param dailyTransactionCount Number of transactions today
     * @param dailyTransactionTotal Total transaction amount today
     * @param newTransactionAmount New transaction amount
     * @throws ValidationException if limits exceeded
     */
    public void validateDailyLimits(int dailyTransactionCount, 
                                    BigDecimal dailyTransactionTotal, 
                                    BigDecimal newTransactionAmount) {
        logger.debug("Validating daily limits: count={}, total={}, new={}", 
                     dailyTransactionCount, dailyTransactionTotal, newTransactionAmount);
        
        List<String> errors = new ArrayList<>();
        
        if (dailyTransactionCount >= MAX_DAILY_TRANSACTIONS) {
            errors.add(String.format("Daily transaction limit of %d transactions exceeded", MAX_DAILY_TRANSACTIONS));
        }
        
        BigDecimal projectedTotal = dailyTransactionTotal.add(newTransactionAmount.abs());
        if (projectedTotal.compareTo(DAILY_TRANSACTION_LIMIT) > 0) {
            errors.add(String.format("Daily transaction amount limit of %s would be exceeded. Current: %s, Requested: %s",
                                     DAILY_TRANSACTION_LIMIT, dailyTransactionTotal, newTransactionAmount));
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("BR-004", "Daily transaction limits exceeded", errors);
        }
    }

    /**
     * Comprehensive transaction validation
     * Orchestrates all validation rules for a transaction request
     */
    public void validateTransaction(Account account, TransactionRequest request, 
                                   int dailyTxCount, BigDecimal dailyTxTotal) {
        logger.info("Performing comprehensive transaction validation for account {}", 
                    request.getAccountNumber());
        
        validateAccountNumber(request.getAccountNumber());
        validateTransactionAmount(request.getAmount());
        validateBalanceSufficiency(account, request.getAmount());
        validateDailyLimits(dailyTxCount, dailyTxTotal, request.getAmount());
        
        logger.info("Transaction validation passed for account {}", request.getAccountNumber());
    }
}