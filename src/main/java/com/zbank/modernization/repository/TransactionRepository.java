package com.zbank.modernization.repository;

import com.zbank.modernization.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Transaction entity persistence and querying.
 * Supports transaction history retrieval, balance calculation, and audit trail requirements.
 * 
 * Traceability:
 * - WF-002: Transaction Processing (debit/credit operations)
 * - WF-003: Transaction History (inquiry and reporting)
 * - NFR-002: Data Integrity (audit trail and consistency)
 * - BR-001: Balance validation before debits
 * - BR-003: Transaction audit trail
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a specific account ordered by timestamp descending.
     * Supports WF-003: Transaction History workflow.
     *
     * @param accountNumber the account number
     * @param pageable pagination parameters
     * @return page of transactions
     */
    Page<Transaction> findByAccountNumberOrderByTransactionTimestampDesc(
            String accountNumber, 
            Pageable pageable
    );

    /**
     * Find transactions for an account within a date range.
     * Supports WF-003: Transaction History with date filtering.
     *
     * @param accountNumber the account number
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of transactions
     */
    Page<Transaction> findByAccountNumberAndTransactionTimestampBetweenOrderByTransactionTimestampDesc(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find transactions by type for an account.
     * Supports filtering by debit/credit for reporting.
     *
     * @param accountNumber the account number
     * @param transactionType transaction type (DEBIT/CREDIT)
     * @param pageable pagination parameters
     * @return page of transactions
     */
    Page<Transaction> findByAccountNumberAndTransactionTypeOrderByTransactionTimestampDesc(
            String accountNumber,
            String transactionType,
            Pageable pageable
    );

    /**
     * Calculate sum of credits for an account within date range.
     * Supports balance reconciliation and reporting (BR-003).
     *
     * @param accountNumber the account number
     * @param startDate start of date range
     * @param endDate end of date range
     * @return sum of credit amounts or zero if no transactions
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionType = 'CREDIT' " +
           "AND t.transactionTimestamp BETWEEN :startDate AND :endDate")
    BigDecimal sumCreditsByAccountNumberAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calculate sum of debits for an account within date range.
     * Supports balance reconciliation and reporting (BR-003).
     *
     * @param accountNumber the account number
     * @param startDate start of date range
     * @param endDate end of date range
     * @return sum of debit amounts or zero if no transactions
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionType = 'DEBIT' " +
           "AND t.transactionTimestamp BETWEEN :startDate AND :endDate")
    BigDecimal sumDebitsByAccountNumberAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find most recent transaction for an account.
     * Supports BR-001: Balance validation by retrieving latest transaction timestamp.
     *
     * @param accountNumber the account number
     * @return optional containing most recent transaction
     */
    Optional<Transaction> findFirstByAccountNumberOrderByTransactionTimestampDesc(
            String accountNumber
    );

    /**
     * Count transactions for an account within date range.
     * Supports transaction volume reporting.
     *
     * @param accountNumber the account number
     * @param startDate start of date range
     * @param endDate end of date range
     * @return count of transactions
     */
    long countByAccountNumberAndTransactionTimestampBetween(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find transactions by reference number for audit and reconciliation.
     * Supports BR-003: Transaction audit trail requirements.
     *
     * @param referenceNumber unique transaction reference
     * @return optional containing transaction if found
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Find all transactions for multiple accounts (batch inquiry).
     * Supports operational reporting across account portfolios.
     *
     * @param accountNumbers list of account numbers
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of transactions
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.accountNumber IN :accountNumbers " +
           "AND t.transactionTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionTimestamp DESC")
    List<Transaction> findByAccountNumbersAndDateRange(
            @Param("accountNumbers") List<String> accountNumbers,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Check if any transactions exist for an account.
     * Supports account closure validation.
     *
     * @param accountNumber the account number
     * @return true if transactions exist
     */
    boolean existsByAccountNumber(String accountNumber);
}