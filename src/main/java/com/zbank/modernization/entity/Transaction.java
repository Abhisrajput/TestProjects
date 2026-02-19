package com.zbank.modernization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Entity - Represents financial transactions with audit trail.
 * 
 * Requirements Traceability:
 * - WF-002: Transaction Processing (Deposit/Withdrawal)
 * - NFR-004: Audit Trail - Complete transaction history with immutable records
 * - NFR-006: Data Consistency - ACID compliance with reconciliation support
 * 
 * Migration Notes:
 * - Replaces VSAM TRANSACT file structure from legacy COBOL system
 * - Maps to SEQDAT.ZBANK transaction records
 * - Preserves COBOL transaction types: DEBIT, CREDIT, TRANSFER
 * - Adds audit fields not present in legacy system
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_reconciliation_status", columnList = "reconciliation_status"),
    @Index(name = "idx_batch_id", columnList = "batch_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_account"))
    private Account account;

    @NotNull
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @NotNull
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 100)
    @Column(name = "reference_number", length = 100, unique = true)
    private String referenceNumber;

    @Column(name = "related_transaction_id")
    private Long relatedTransactionId;

    @Size(max = 50)
    @Column(name = "batch_id", length = 50)
    private String batchId;

    @NotNull
    @Column(name = "reconciliation_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @Column(name = "reconciliation_date")
    private LocalDateTime reconciliationDate;

    @Size(max = 100)
    @Column(name = "reconciled_by", length = 100)
    private String reconciledBy;

    @Size(max = 100)
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 200)
    @Column(name = "user_agent", length = 200)
    private String userAgent;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Size(max = 500)
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Size(max = 100)
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Size(max = 100)
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Transaction Type Enumeration
     * Maps to legacy COBOL transaction codes:
     * - DEBIT (D): Withdrawal/Debit
     * - CREDIT (C): Deposit/Credit
     * - TRANSFER_OUT (TO): Outgoing transfer
     * - TRANSFER_IN (TI): Incoming transfer
     * - FEE (F): Service fee
     * - REVERSAL (R): Transaction reversal
     */
    public enum TransactionType {
        DEBIT,
        CREDIT,
        TRANSFER_OUT,
        TRANSFER_IN,
        FEE,
        INTEREST,
        REVERSAL,
        ADJUSTMENT
    }

    /**
     * Transaction Status Enumeration
     * Supports transaction lifecycle beyond legacy COBOL capabilities
     */
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REVERSED,
        CANCELLED
    }

    /**
     * Reconciliation Status Enumeration
     * Enables end-of-day reconciliation processes
     */
    public enum ReconciliationStatus {
        PENDING,
        RECONCILED,
        DISCREPANCY,
        EXCLUDED
    }

    /**
     * Pre-persist callback to set audit fields
     */
    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (referenceNumber == null) {
            referenceNumber = generateReferenceNumber();
        }
    }

    /**
     * Generate unique reference number for transaction
     * Format: TXN-{YYYYMMDDHHMMSS}-{accountId}-{randomDigits}
     */
    private String generateReferenceNumber() {
        String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 14);
        String accountSuffix = account != null ? String.valueOf(account.getAccountId()) : "0";
        String random = String.valueOf((int)(Math.random() * 10000));
        return String.format("TXN-%s-%s-%s", timestamp, accountSuffix, random);
    }

    /**
     * Check if transaction is reversible
     */
    public boolean isReversible() {
        return status == TransactionStatus.COMPLETED 
            && transactionType != TransactionType.REVERSAL
            && reconciliationStatus == ReconciliationStatus.PENDING;
    }

    /**
     * Check if transaction affects account balance
     */
    public boolean affectsBalance() {
        return status == TransactionStatus.COMPLETED 
            && transactionType != TransactionType.REVERSAL;
    }
}