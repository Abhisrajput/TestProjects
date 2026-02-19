package com.zbank.modernization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for Account Inquiry Response
 * Maps to legacy ZBANK COBOL output structure
 * Traceability: WF-001 Account Inquiry, REQ-001, REQ-002
 * 
 * Legacy mapping:
 * - ACCOUNT-NUMBER -> accountNumber
 * - ACCOUNT-BALANCE -> balance
 * - ACCOUNT-STATUS -> accountStatus
 * - LAST-TRANSACTION-DATE -> lastTransactionDate
 */
@Schema(description = "Account inquiry response containing balance and status information")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountInquiryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Account number", example = "1234567890", required = true)
    @JsonProperty("accountNumber")
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @Schema(description = "Account holder name", example = "John Smith", required = true)
    @JsonProperty("accountName")
    @NotBlank(message = "Account name is required")
    private String accountName;

    @Schema(description = "Current account balance", example = "15000.00", required = true)
    @JsonProperty("balance")
    @NotNull(message = "Balance is required")
    private BigDecimal balance;

    @Schema(description = "Available balance for withdrawal", example = "14500.00", required = true)
    @JsonProperty("availableBalance")
    @NotNull(message = "Available balance is required")
    private BigDecimal availableBalance;

    @Schema(description = "Account status", example = "ACTIVE", required = true, allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED", "CLOSED"})
    @JsonProperty("accountStatus")
    @NotBlank(message = "Account status is required")
    private String accountStatus;

    @Schema(description = "Account type", example = "CHECKING", allowableValues = {"CHECKING", "SAVINGS", "MONEY_MARKET"})
    @JsonProperty("accountType")
    private String accountType;

    @Schema(description = "Currency code", example = "USD", required = true)
    @JsonProperty("currency")
    @NotBlank(message = "Currency is required")
    private String currency;

    @Schema(description = "Last transaction date and time", example = "2024-01-15T14:30:00")
    @JsonProperty("lastTransactionDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastTransactionDate;

    @Schema(description = "Account opening date", example = "2020-03-10T09:00:00")
    @JsonProperty("openDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime openDate;

    @Schema(description = "Interest rate for savings accounts", example = "2.5")
    @JsonProperty("interestRate")
    private BigDecimal interestRate;

    @Schema(description = "Branch code", example = "B001")
    @JsonProperty("branchCode")
    private String branchCode;

    @Schema(description = "Response timestamp", example = "2024-01-20T10:15:30", required = true)
    @JsonProperty("responseTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @NotNull(message = "Response timestamp is required")
    private LocalDateTime responseTimestamp;

    @Schema(description = "Transaction reference number", example = "TXN-20240120-001")
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    public AccountInquiryResponse() {
        this.responseTimestamp = LocalDateTime.now();
        this.currency = "USD";
    }

    public AccountInquiryResponse(String accountNumber, String accountName, BigDecimal balance, 
                                 BigDecimal availableBalance, String accountStatus) {
        this();
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.accountStatus = accountStatus;
    }

    // Getters and Setters

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(LocalDateTime lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public LocalDateTime getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDateTime openDate) {
        this.openDate = openDate;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public LocalDateTime getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(LocalDateTime responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountInquiryResponse that = (AccountInquiryResponse) o;
        return Objects.equals(accountNumber, that.accountNumber) &&
               Objects.equals(referenceNumber, that.referenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber, referenceNumber);
    }

    @Override
    public String toString() {
        return "AccountInquiryResponse{" +
                "accountNumber='" + accountNumber + '\'' +
                ", accountName='" + accountName + '\'' +
                ", balance=" + balance +
                ", availableBalance=" + availableBalance +
                ", accountStatus='" + accountStatus + '\'' +
                ", accountType='" + accountType + '\'' +
                ", currency='" + currency + '\'' +
                ", lastTransactionDate=" + lastTransactionDate +
                ", responseTimestamp=" + responseTimestamp +
                ", referenceNumber='" + referenceNumber + '\'' +
                '}';
    }
}