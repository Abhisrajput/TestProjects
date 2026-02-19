package com.zbank.modernization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity mapping account data from VSAM to relational database schema.
 * 
 * Traceability:
 * - REQ-001: Account Inquiry - Maps VSAM account record structure
 * - REQ-002: Balance Inquiry - Stores current and available balance
 * - REQ-003: Transaction Processing - Maintains transaction-affecting fields
 * - WF-001: Account Inquiry workflow data requirements
 * 
 * Migration Notes:
 * - VSAM Key: ACCT-ID (9 digits) -> accountNumber (String)
 * - COBOL PIC S9(13)V99 COMP-3 -> BigDecimal with precision 15, scale 2
 * - COBOL date fields (YYYYMMDD) -> LocalDate
 * - Status codes preserved from legacy system
 */
@Entity
@Table(name = "accounts", 
       indexes = {
           @Index(name = "idx_account_number", columnList = "account_number", unique = true),
           @Index(name = "idx_customer_id", columnList = "customer_id"),
           @Index(name = "idx_account_status", columnList = "status")
       })
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Legacy VSAM ACCT-ID field - 9 digit account identifier
     */
    @Column(name = "account_number", nullable = false, unique = true, length = 9)
    @NotNull(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{9}$", message = "Account number must be 9 digits")
    private String accountNumber;

    /**
     * Customer identifier - links to customer master (future implementation)
     */
    @Column(name = "customer_id", nullable = false, length = 20)
    @NotNull(message = "Customer ID is required")
    @Size(max = 20)
    private String customerId;

    /**
     * Account holder name from VSAM ACCT-NAME field
     */
    @Column(name = "account_name", nullable = false, length = 60)
    @NotBlank(message = "Account name is required")
    @Size(max = 60)
    private String accountName;

    /**
     * Account type code:
     * - CHK: Checking Account
     * - SAV: Savings Account
     * - LON: Loan Account
     * Mapped from COBOL ACCT-TYPE (PIC X(3))
     */
    @Column(name = "account_type", nullable = false, length = 3)
    @NotNull(message = "Account type is required")
    @Pattern(regexp = "^(CHK|SAV|LON)$", message = "Invalid account type")
    private String accountType;

    /**
     * Current balance - COBOL ACCT-BALANCE PIC S9(13)V99 COMP-3
     * Precision: 15 digits, Scale: 2 decimal places
     */
    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Current balance is required")
    private BigDecimal currentBalance;

    /**
     * Available balance (after pending holds/transactions)
     * COBOL ACCT-AVAIL-BAL PIC S9(13)V99 COMP-3
     */
    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Available balance is required")
    private BigDecimal availableBalance;

    /**
     * Account opening date - COBOL ACCT-OPEN-DATE PIC 9(8) (YYYYMMDD)
     */
    @Column(name = "open_date", nullable = false)
    @NotNull(message = "Open date is required")
    private LocalDate openDate;

    /**
     * Last transaction date - COBOL ACCT-LAST-TXN-DATE PIC 9(8)
     */
    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    /**
     * Account status:
     * - A: Active
     * - C: Closed
     * - F: Frozen
     * - D: Dormant
     * Mapped from COBOL ACCT-STATUS PIC X(1)
     */
    @Column(name = "status", nullable = false, length = 1)
    @NotNull(message = "Status is required")
    @Pattern(regexp = "^[ACFD]$", message = "Invalid status code")
    private String status;

    /**
     * Branch code - COBOL ACCT-BRANCH PIC X(4)
     */
    @Column(name = "branch_code", length = 4)
    @Size(max = 4)
    private String branchCode;

    /**
     * Interest rate for savings/loan accounts - COBOL ACCT-INT-RATE PIC 9V9(4)
     */
    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    /**
     * Overdraft limit for checking accounts
     */
    @Column(name = "overdraft_limit", precision = 15, scale = 2)
    private BigDecimal overdraftLimit;

    /**
     * Currency code (default USD) - future multi-currency support
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    @NotNull
    private String currencyCode = "USD";

    /**
     * Audit fields
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /**
     * Version for optimistic locking - prevents concurrent update conflicts
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "A"; // Default to Active
        }
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Account() {
    }

    public Account(String accountNumber, String customerId, String accountName, String accountType) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.currentBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.openDate = LocalDate.now();
        this.status = "A";
        this.currencyCode = "USD";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(LocalDate lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Business methods
    public boolean isActive() {
        return "A".equals(status);
    }

    public boolean isClosed() {
        return "C".equals(status);
    }

    public boolean isFrozen() {
        return "F".equals(status);
    }

    public boolean canTransact() {
        return isActive() && !isFrozen();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountName='" + accountName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", currentBalance=" + currentBalance +
                ", status='" + status + '\'' +
                ", version=" + version +
                '}';
    }
}