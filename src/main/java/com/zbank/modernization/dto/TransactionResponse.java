package com.zbank.modernization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for transaction responses.
 * Maps to legacy COBOL COMMAREA transaction result fields.
 * 
 * Traceability:
 * - Stage 1: WF-002 (Transaction Processing)
 * - Stage 1: BR-004 (Balance validation)
 * - Legacy: ZBANK3.cbl COMMAREA structure
 * - Stage 4: RESTful API response contract
 */
@Schema(description = "Transaction processing response with confirmation and updated balance")
public class TransactionResponse {

    @Schema(description = "Unique transaction confirmation number", example = "TXN20240115123456789")
    @NotBlank(message = "Transaction ID is required")
    @JsonProperty("transactionId")
    private String transactionId;

    @Schema(description = "Account number", example = "0000012345")
    @NotBlank(message = "Account number is required")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @Schema(description = "Transaction type", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
    @NotBlank(message = "Transaction type is required")
    @JsonProperty("transactionType")
    private String transactionType;

    @Schema(description = "Transaction amount", example = "150.00")
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @JsonProperty("transactionAmount")
    private BigDecimal transactionAmount;

    @Schema(description = "Account balance before transaction", example = "1000.00")
    @NotNull(message = "Previous balance is required")
    @JsonProperty("previousBalance")
    private BigDecimal previousBalance;

    @Schema(description = "Account balance after transaction", example = "1150.00")
    @NotNull(message = "New balance is required")
    @JsonProperty("newBalance")
    private BigDecimal newBalance;

    @Schema(description = "Transaction timestamp", example = "2024-01-15T12:34:56")
    @NotNull(message = "Transaction timestamp is required")
    @JsonProperty("transactionTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionTimestamp;

    @Schema(description = "Transaction status", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "PENDING"})
    @NotBlank(message = "Status is required")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Response message", example = "Transaction completed successfully")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Response code from core banking system", example = "0000")
    @JsonProperty("responseCode")
    private String responseCode;

    /**
     * Default constructor for Jackson deserialization.
     */
    public TransactionResponse() {
    }

    /**
     * Full constructor for programmatic creation.
     */
    public TransactionResponse(String transactionId, String accountNumber, String transactionType,
                              BigDecimal transactionAmount, BigDecimal previousBalance,
                              BigDecimal newBalance, LocalDateTime transactionTimestamp,
                              String status, String message, String responseCode) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.transactionAmount = transactionAmount;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.transactionTimestamp = transactionTimestamp;
        this.status = status;
        this.message = message;
        this.responseCode = responseCode;
    }

    // Getters and Setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionResponse that = (TransactionResponse) o;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(accountNumber, that.accountNumber) &&
               Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(transactionAmount, that.transactionAmount) &&
               Objects.equals(previousBalance, that.previousBalance) &&
               Objects.equals(newBalance, that.newBalance) &&
               Objects.equals(transactionTimestamp, that.transactionTimestamp) &&
               Objects.equals(status, that.status) &&
               Objects.equals(responseCode, that.responseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountNumber, transactionType, transactionAmount,
                          previousBalance, newBalance, transactionTimestamp, status, responseCode);
    }

    @Override
    public String toString() {
        return "TransactionResponse{" +
                "transactionId='" + transactionId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", transactionAmount=" + transactionAmount +
                ", previousBalance=" + previousBalance +
                ", newBalance=" + newBalance +
                ", transactionTimestamp=" + transactionTimestamp +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", responseCode='" + responseCode + '\'' +
                '}';
    }
}