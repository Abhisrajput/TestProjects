package com.zbank.modernization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for transaction requests.
 * Supports deposit, withdrawal, and transfer operations.
 * 
 * Traceability:
 * - Maps to legacy COBOL transaction processing in ZBANK3
 * - Supports WF-002 (Deposit), WF-003 (Withdrawal), WF-004 (Transfer)
 * - Replaces VSAM record updates with REST-based transaction API
 */
public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    @Pattern(regexp = "^(DEPOSIT|WITHDRAWAL|TRANSFER)$", 
             message = "Transaction type must be DEPOSIT, WITHDRAWAL, or TRANSFER")
    @JsonProperty("transactionType")
    private String transactionType;

    @NotBlank(message = "Source account number is required")
    @Pattern(regexp = "^[0-9]{10}$", 
             message = "Account number must be 10 digits")
    @JsonProperty("sourceAccountNumber")
    private String sourceAccountNumber;

    @Pattern(regexp = "^[0-9]{10}$", 
             message = "Target account number must be 10 digits")
    @JsonProperty("targetAccountNumber")
    private String targetAccountNumber;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @DecimalMax(value = "999999.99", message = "Amount exceeds maximum limit")
    @Digits(integer = 6, fraction = 2, message = "Amount must have at most 6 integer and 2 decimal digits")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Size(max = 100, message = "Description cannot exceed 100 characters")
    @JsonProperty("description")
    private String description;

    @NotBlank(message = "Operator ID is required")
    @Size(min = 3, max = 8, message = "Operator ID must be 3-8 characters")
    @JsonProperty("operatorId")
    private String operatorId;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    // Constructors
    public TransactionRequest() {
        this.timestamp = LocalDateTime.now();
    }

    public TransactionRequest(String transactionType, String sourceAccountNumber, 
                             BigDecimal amount, String operatorId) {
        this.transactionType = transactionType;
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
        this.operatorId = operatorId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    // Business validation methods
    public boolean isTransfer() {
        return "TRANSFER".equals(transactionType);
    }

    public boolean isDeposit() {
        return "DEPOSIT".equals(transactionType);
    }

    public boolean isWithdrawal() {
        return "WITHDRAWAL".equals(transactionType);
    }

    public boolean requiresTargetAccount() {
        return isTransfer();
    }

    // Object overrides
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionRequest that = (TransactionRequest) o;
        return Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(sourceAccountNumber, that.sourceAccountNumber) &&
               Objects.equals(targetAccountNumber, that.targetAccountNumber) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(operatorId, that.operatorId) &&
               Objects.equals(referenceNumber, that.referenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionType, sourceAccountNumber, targetAccountNumber, 
                          amount, operatorId, referenceNumber);
    }

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "transactionType='" + transactionType + '\'' +
                ", sourceAccountNumber='" + maskAccount(sourceAccountNumber) + '\'' +
                ", targetAccountNumber='" + maskAccount(targetAccountNumber) + '\'' +
                ", amount=" + amount +
                ", operatorId='" + operatorId + '\'' +
                ", timestamp=" + timestamp +
                ", referenceNumber='" + referenceNumber + '\'' +
                '}';
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "******" + accountNumber.substring(accountNumber.length() - 4);
    }
}