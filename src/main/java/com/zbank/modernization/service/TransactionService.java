package com.zbank.modernization.service;

import com.zbank.modernization.dto.TransactionRequest;
import com.zbank.modernization.dto.TransactionResponse;
import com.zbank.modernization.entity.Account;
import com.zbank.modernization.entity.Transaction;
import com.zbank.modernization.entity.TransactionType;
import com.zbank.modernization.exception.InsufficientFundsException;
import com.zbank.modernization.exception.InvalidTransactionException;
import com.zbank.modernization.exception.AccountNotFoundException;
import com.zbank.modernization.repository.AccountRepository;
import com.zbank.modernization.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction processing service for deposits, withdrawals, and transfers.
 * Implements business rules derived from legacy COBOL ZBANK3 program.
 * 
 * Traceability:
 * - WF-002: Balance Update (Deposit/Withdrawal)
 * - WF-003: Account Transfer
 * - BR-002: Overdraft Prevention
 * - BR-003: Transaction Amount Validation
 * - NFR-004: Transaction Atomicity
 */
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("999999.99");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    /**
     * Process deposit transaction.
     * Maps to legacy COBOL deposit function (OPCODE='D').
     * 
     * @param request Transaction request containing account and amount
     * @return TransactionResponse with updated balance
     * @throws AccountNotFoundException if account does not exist
     * @throws InvalidTransactionException if amount validation fails
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse processDeposit(TransactionRequest request) {
        logger.info("Processing deposit for account: {}, amount: {}", 
                    request.getAccountNumber(), request.getAmount());
        
        validateTransactionAmount(request.getAmount());
        
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                    "Account not found: " + request.getAccountNumber()));
        
        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance = previousBalance.add(request.getAmount());
        
        account.setBalance(newBalance);
        account.setLastTransactionDate(LocalDateTime.now());
        accountRepository.save(account);
        
        Transaction transaction = createTransaction(
            account, 
            TransactionType.DEPOSIT, 
            request.getAmount(), 
            previousBalance, 
            newBalance,
            request.getDescription()
        );
        
        transactionRepository.save(transaction);
        
        logger.info("Deposit successful. Transaction ID: {}, New balance: {}", 
                    transaction.getTransactionId(), newBalance);
        
        return buildTransactionResponse(transaction, account);
    }

    /**
     * Process withdrawal transaction with overdraft validation.
     * Maps to legacy COBOL withdrawal function (OPCODE='W').
     * 
     * @param request Transaction request containing account and amount
     * @return TransactionResponse with updated balance
     * @throws AccountNotFoundException if account does not exist
     * @throws InvalidTransactionException if amount validation fails
     * @throws InsufficientFundsException if balance insufficient (BR-002)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse processWithdrawal(TransactionRequest request) {
        logger.info("Processing withdrawal for account: {}, amount: {}", 
                    request.getAccountNumber(), request.getAmount());
        
        validateTransactionAmount(request.getAmount());
        
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                    "Account not found: " + request.getAccountNumber()));
        
        BigDecimal previousBalance = account.getBalance();
        
        // BR-002: Overdraft Prevention
        if (previousBalance.compareTo(request.getAmount()) < 0) {
            logger.warn("Insufficient funds for withdrawal. Account: {}, Balance: {}, Requested: {}",
                       request.getAccountNumber(), previousBalance, request.getAmount());
            throw new InsufficientFundsException(
                String.format("Insufficient funds. Available: %s, Requested: %s", 
                             previousBalance, request.getAmount()));
        }
        
        BigDecimal newBalance = previousBalance.subtract(request.getAmount());
        
        account.setBalance(newBalance);
        account.setLastTransactionDate(LocalDateTime.now());
        accountRepository.save(account);
        
        Transaction transaction = createTransaction(
            account, 
            TransactionType.WITHDRAWAL, 
            request.getAmount(), 
            previousBalance, 
            newBalance,
            request.getDescription()
        );
        
        transactionRepository.save(transaction);
        
        logger.info("Withdrawal successful. Transaction ID: {}, New balance: {}", 
                    transaction.getTransactionId(), newBalance);
        
        return buildTransactionResponse(transaction, account);
    }

    /**
     * Process transfer between two accounts.
     * Maps to legacy COBOL transfer function (OPCODE='T').
     * Implements atomic two-phase transfer with rollback capability.
     * 
     * @param request Transaction request with source, target accounts and amount
     * @return TransactionResponse for source account debit
     * @throws AccountNotFoundException if either account does not exist
     * @throws InvalidTransactionException if amount validation fails or same account
     * @throws InsufficientFundsException if source balance insufficient
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse processTransfer(TransactionRequest request) {
        logger.info("Processing transfer from {} to {}, amount: {}", 
                    request.getAccountNumber(), 
                    request.getTargetAccountNumber(), 
                    request.getAmount());
        
        validateTransactionAmount(request.getAmount());
        
        if (request.getAccountNumber().equals(request.getTargetAccountNumber())) {
            throw new InvalidTransactionException("Cannot transfer to same account");
        }
        
        // Load both accounts with pessimistic locking
        Account sourceAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                    "Source account not found: " + request.getAccountNumber()));
        
        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                    "Target account not found: " + request.getTargetAccountNumber()));
        
        BigDecimal sourceBalance = sourceAccount.getBalance();
        
        // BR-002: Overdraft Prevention for source account
        if (sourceBalance.compareTo(request.getAmount()) < 0) {
            logger.warn("Insufficient funds for transfer. Account: {}, Balance: {}, Requested: {}",
                       request.getAccountNumber(), sourceBalance, request.getAmount());
            throw new InsufficientFundsException(
                String.format("Insufficient funds in source account. Available: %s, Requested: %s", 
                             sourceBalance, request.getAmount()));
        }
        
        // Debit source account
        BigDecimal newSourceBalance = sourceBalance.subtract(request.getAmount());
        sourceAccount.setBalance(newSourceBalance);
        sourceAccount.setLastTransactionDate(LocalDateTime.now());
        
        // Credit target account
        BigDecimal targetBalance = targetAccount.getBalance();
        BigDecimal newTargetBalance = targetBalance.add(request.getAmount());
        targetAccount.setBalance(newTargetBalance);
        targetAccount.setLastTransactionDate(LocalDateTime.now());
        
        // Save both accounts atomically
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);
        
        // Create linked transaction records
        String transferReference = UUID.randomUUID().toString();
        
        Transaction debitTransaction = createTransferTransaction(
            sourceAccount, 
            TransactionType.TRANSFER_OUT, 
            request.getAmount(), 
            sourceBalance, 
            newSourceBalance,
            "Transfer to " + request.getTargetAccountNumber(),
            transferReference
        );
        
        Transaction creditTransaction = createTransferTransaction(
            targetAccount, 
            TransactionType.TRANSFER_IN, 
            request.getAmount(), 
            targetBalance, 
            newTargetBalance,
            "Transfer from " + request.getAccountNumber(),
            transferReference
        );
        
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);
        
        logger.info("Transfer successful. Debit TX: {}, Credit TX: {}, Reference: {}", 
                    debitTransaction.getTransactionId(), 
                    creditTransaction.getTransactionId(),
                    transferReference);
        
        return buildTransactionResponse(debitTransaction, sourceAccount);
    }

    /**
     * Validate transaction amount against business rules.
     * BR-003: Transaction Amount Validation
     * 
     * @param amount Transaction amount to validate
     * @throws InvalidTransactionException if validation fails
     */
    private void validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("Transaction amount cannot be null");
        }
        
        if (amount.compareTo(ZERO) <= 0) {
            throw new InvalidTransactionException(
                "Transaction amount must be positive: " + amount);
        }
        
        if (amount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
            throw new InvalidTransactionException(
                String.format("Transaction amount below minimum: %s (min: %s)", 
                             amount, MIN_TRANSACTION_AMOUNT));
        }
        
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new InvalidTransactionException(
                String.format("Transaction amount exceeds maximum: %s (max: %s)", 
                             amount, MAX_TRANSACTION_AMOUNT));
        }
        
        // Validate scale (max 2 decimal places)
        if (amount.scale() > 2) {
            throw new InvalidTransactionException(
                "Transaction amount cannot have more than 2 decimal places: " + amount);
        }
    }

    /**
     * Create transaction record.
     */
    private Transaction createTransaction(
            Account account,
            TransactionType type,
            BigDecimal amount,
            BigDecimal previousBalance,
            BigDecimal newBalance,
            String description) {
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setPreviousBalance(previousBalance);
        transaction.setNewBalance(newBalance);
        transaction.setDescription(description);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("COMPLETED");
        
        return transaction;
    }

    /**
     * Create transfer transaction with reference linking.
     */
    private Transaction createTransferTransaction(
            Account account,
            TransactionType type,
            BigDecimal amount,
            BigDecimal previousBalance,
            BigDecimal newBalance,
            String description,
            String transferReference) {
        
        Transaction transaction = createTransaction(
            account, type, amount, previousBalance, newBalance, description);
        transaction.setTransferReference(transferReference);
        
        return transaction;
    }

    /**
     * Build standardized transaction response.
     */
    private TransactionResponse buildTransactionResponse(
            Transaction transaction, 
            Account account) {
        
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setAccountNumber(account.getAccountNumber());
        response.setTransactionType(transaction.getTransactionType().name());
        response.setAmount(transaction.getAmount());
        response.setPreviousBalance(transaction.getPreviousBalance());
        response.setNewBalance(transaction.getNewBalance());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setTransferReference(transaction.getTransferReference());
        
        return response;
    }
}