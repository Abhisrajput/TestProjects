package com.zbank.modernization.repository;

import com.zbank.modernization.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Account entity.
 * Provides data access operations for account management.
 * 
 * Traceability:
 * - Maps to WF-001 (Account Inquiry): findByAccountNumber
 * - Maps to WF-002 (Balance Update): pessimistic locking for concurrency
 * - Maps to WF-003 (Transaction Processing): balance validation queries
 * - Replaces legacy VSAM ACCTVS file access from ZBANK3.cbl
 * 
 * Migration Notes:
 * - VSAM key (account number) mapped to indexed accountNumber field
 * - Supports concurrent access patterns required by CICS transaction model
 * - Implements optimistic/pessimistic locking for balance updates
 * 
 * @author zBANK Modernization Team
 * @version 1.0
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by account number (primary business key).
     * Replaces VSAM ACCTVS READ by account number.
     * 
     * @param accountNumber unique account identifier (PIC X(10) in COBOL)
     * @return Optional containing account if found
     * Traceability: WF-001 (Account Inquiry), REQ-001
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account with pessimistic write lock for balance updates.
     * Ensures serialized access to prevent concurrent modification issues.
     * Replaces CICS ENQ/DEQ mechanism from legacy system.
     * 
     * @param accountNumber unique account identifier
     * @return Optional containing locked account if found
     * Traceability: WF-002 (Balance Update), WF-003 (Transaction Processing)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    /**
     * Check if account exists by account number.
     * Lightweight existence check without loading full entity.
     * 
     * @param accountNumber unique account identifier
     * @return true if account exists
     * Traceability: REQ-002 (Account Validation)
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find all accounts by customer ID.
     * Supports customer-level account listing.
     * 
     * @param customerId customer identifier
     * @return list of accounts owned by customer
     * Traceability: REQ-005 (Multi-account Management)
     */
    List<Account> findByCustomerId(String customerId);

    /**
     * Find accounts with balance greater than specified amount.
     * Supports reporting and analytics requirements.
     * 
     * @param minBalance minimum balance threshold
     * @return list of accounts meeting criteria
     * Traceability: REQ-007 (Balance Reporting)
     */
    @Query("SELECT a FROM Account a WHERE a.balance >= :minBalance")
    List<Account> findAccountsWithMinimumBalance(@Param("minBalance") BigDecimal minBalance);

    /**
     * Find accounts modified after specified timestamp.
     * Supports incremental data synchronization and audit.
     * 
     * @param lastModified cutoff timestamp
     * @return list of recently modified accounts
     * Traceability: REQ-008 (Audit Trail)
     */
    @Query("SELECT a FROM Account a WHERE a.lastModified > :lastModified ORDER BY a.lastModified ASC")
    List<Account> findRecentlyModifiedAccounts(@Param("lastModified") LocalDateTime lastModified);

    /**
     * Find active accounts (not closed or suspended).
     * Supports operational queries excluding inactive accounts.
     * 
     * @return list of active accounts
     * Traceability: REQ-004 (Account Status Management)
     */
    @Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE' ORDER BY a.accountNumber")
    List<Account> findActiveAccounts();

    /**
     * Count accounts by status.
     * Supports dashboard and reporting metrics.
     * 
     * @param status account status value
     * @return count of accounts with specified status
     * Traceability: REQ-009 (System Metrics)
     */
    long countByStatus(String status);

    /**
     * Find accounts by customer ID and status.
     * Composite query for filtered customer account listing.
     * 
     * @param customerId customer identifier
     * @param status account status
     * @return list of matching accounts
     * Traceability: REQ-005, REQ-004
     */
    List<Account> findByCustomerIdAndStatus(String customerId, String status);

    /**
     * Validate sufficient balance for transaction.
     * Business rule validation from COBOL PERFORM CHECK-BALANCE.
     * 
     * @param accountNumber account to check
     * @param requiredAmount amount needed
     * @return true if balance is sufficient
     * Traceability: WF-003 (Transaction Processing), BR-001 (Sufficient Funds)
     */
    @Query("SELECT CASE WHEN a.balance >= :requiredAmount THEN true ELSE false END " +
           "FROM Account a WHERE a.accountNumber = :accountNumber")
    Boolean hasSufficientBalance(@Param("accountNumber") String accountNumber,
                                  @Param("requiredAmount") BigDecimal requiredAmount);

    /**
     * Get total balance across all accounts for customer.
     * Aggregation query for customer portfolio view.
     * 
     * @param customerId customer identifier
     * @return sum of all account balances
     * Traceability: REQ-006 (Customer Portfolio)
     */
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.customerId = :customerId")
    BigDecimal getTotalBalanceByCustomer(@Param("customerId") String customerId);
}