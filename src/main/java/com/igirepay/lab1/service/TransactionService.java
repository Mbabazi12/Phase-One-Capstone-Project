package com.igirepay.lab1.service;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountNotFoundException;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.DuplicateTransactionException;
import com.igirepay.lab1.model.InsufficientBalanceException;
import com.igirepay.lab1.model.InvalidAmountException;
import com.igirepay.lab1.model.InvalidPinException;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionStatus;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab1.model.WalletAccount;
import com.igirepay.lab1.model.WithdrawalLimitExceededException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TransactionService {
    private final Set<String> processedReferenceIds;
    private final List<Transaction> globalFailedLog;
    private final Map<UUID, List<Transaction>> historyByAccountId;

    public TransactionService() {
        this.processedReferenceIds = new HashSet<>();
        this.globalFailedLog = new ArrayList<>();
        this.historyByAccountId = new HashMap<>();
    }

    public Transaction deposit(Account account, BigDecimal amount, String referenceId) {
        String normalizedReferenceId = requireNewReference(referenceId, account, null, TransactionType.DEPOSIT, amount);
        requireAccount(account);
        BigDecimal normalizedAmount = requirePositiveAmount(amount);

        try {
            Transaction transaction = account.deposit(normalizedAmount, normalizedReferenceId);
            markProcessed(normalizedReferenceId);
            logSuccess(transaction);
            return transaction;
        } catch (RuntimeException exception) {
            logFailed(account, null, TransactionType.DEPOSIT, normalizedAmount, BigDecimal.ZERO,
                    normalizedReferenceId, exception.getMessage());
            throw exception;
        }
    }

    public Transaction withdraw(Account account, BigDecimal amount, String pin, String referenceId) {
        String normalizedReferenceId = requireNewReference(referenceId, account, null, TransactionType.WITHDRAWAL, amount);
        requireAccount(account);

        try {
            validateAccountPin(account, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            ensureSufficientBalance(account, normalizedAmount);
            if (account instanceof SavingsAccount) {
                ensureSavingsWithdrawalLimit(account.getAccountId());
            }

            Transaction transaction = account.withdraw(normalizedAmount, normalizedReferenceId);
            markProcessed(normalizedReferenceId);
            logSuccess(transaction);
            return transaction;
        } catch (RuntimeException exception) {
            logFailed(account, null, TransactionType.WITHDRAWAL,
                    amount == null ? BigDecimal.ZERO : amount, BigDecimal.ZERO,
                    normalizedReferenceId, exception.getMessage());
            throw exception;
        }
    }

    public List<Transaction> transfer(Account sender, String recipientPhone, BigDecimal amount, String pin,
                                      String referenceId, CustomerService customerService) {
        String normalizedReferenceId = requireNewReference(referenceId, sender, null, TransactionType.TRANSFER_OUT, amount);
        requireAccount(sender);
        Customer recipient = findRecipient(recipientPhone, customerService);

        Account recipientAccount = null;
        try {
            validateAccountPin(sender, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            recipientAccount = resolveRecipientAccount(sender, recipient);
            BigDecimal fee = calculateTransferFee(sender, recipient, normalizedAmount);
            ensureSufficientBalance(sender, normalizedAmount.add(fee));

            Transaction transferOut = new Transaction(
                    normalizedReferenceId,
                    sender.getAccountId(),
                    recipientAccount.getAccountId(),
                    TransactionType.TRANSFER_OUT,
                    normalizedAmount,
                    "Transfer to " + recipient.getFullName() + " (" + recipient.getPhoneNumber() + ")"
            );
            Transaction transferIn = new Transaction(
                    normalizedReferenceId,
                    recipientAccount.getAccountId(),
                    sender.getAccountId(),
                    TransactionType.TRANSFER_IN,
                    normalizedAmount,
                    "Transfer from account " + sender.getAccountId()
            );

            List<Transaction> transactions = new ArrayList<>();
            sender.processTransaction(transferOut);
            logSuccess(transferOut);
            transactions.add(transferOut);

            recipientAccount.processTransaction(transferIn);
            logSuccess(transferIn);
            transactions.add(transferIn);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                Transaction feeTransaction = new Transaction(
                        normalizedReferenceId,
                        sender.getAccountId(),
                        null,
                        TransactionType.FEE,
                        fee,
                        "Transfer fee"
                );
                sender.processTransaction(feeTransaction);
                logSuccess(feeTransaction);
                transactions.add(feeTransaction);
            }

            markProcessed(normalizedReferenceId);
            return Collections.unmodifiableList(transactions);
        } catch (RuntimeException exception) {
            logFailed(sender,
                    recipientAccount == null ? null : recipientAccount.getAccountId(),
                    TransactionType.TRANSFER_OUT,
                    amount == null ? BigDecimal.ZERO : amount,
                    BigDecimal.ZERO,
                    normalizedReferenceId,
                    exception.getMessage());
            throw exception;
        }
    }

    public String lookupRecipientName(String phone, CustomerService customerService) {
        return findRecipient(phone, customerService).getFullName();
    }

    public BigDecimal previewTransferFee(Account sender, String recipientPhone, BigDecimal amount,
                                         CustomerService customerService) {
        requireAccount(sender);
        Customer recipient = findRecipient(recipientPhone, customerService);
        BigDecimal normalizedAmount = requirePositiveAmount(amount);
        resolveRecipientAccount(sender, recipient);
        return calculateTransferFee(sender, recipient, normalizedAmount);
    }

    public BigDecimal previewTransferTotal(Account sender, String recipientPhone, BigDecimal amount,
                                           CustomerService customerService) {
        BigDecimal normalizedAmount = requirePositiveAmount(amount);
        return normalizedAmount.add(previewTransferFee(sender, recipientPhone, normalizedAmount, customerService));
    }

    public void validateAccountPin(Account account, String rawPin) {
        requireAccount(account);
        AuthService.validatePinFormat(rawPin);
        if (!AuthService.hashPin(rawPin).equals(account.getHashedPin())) {
            throw new InvalidPinException(0);
        }
    }

    public List<Transaction> getHistory(UUID accountId) {
        return Collections.unmodifiableList(historyByAccountId.getOrDefault(accountId, List.of()));
    }

    public List<Transaction> getDailyWithdrawals(UUID accountId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        List<Transaction> withdrawals = getHistory(accountId).stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .filter(transaction -> transaction.getTimestamp().toLocalDate().equals(targetDate))
                .toList();
        return Collections.unmodifiableList(withdrawals);
    }

    public Set<String> getProcessedReferenceIds() {
        return Collections.unmodifiableSet(processedReferenceIds);
    }

    public List<Transaction> getGlobalFailedLog() {
        return Collections.unmodifiableList(globalFailedLog);
    }

    public boolean isReferenceProcessed(String referenceId) {
        return processedReferenceIds.contains(normalizeReference(referenceId));
    }

    private String requireNewReference(String referenceId, Account account, UUID targetAccountId,
                                       TransactionType transactionType, BigDecimal amount) {
        String normalizedReferenceId = normalizeReference(referenceId);
        if (normalizedReferenceId.isEmpty()) {
            throw new IllegalArgumentException("Reference ID is required.");
        }
        if (processedReferenceIds.contains(normalizedReferenceId)) {
            Transaction duplicate = createStatusTransaction(account, targetAccountId, transactionType,
                    amount, BigDecimal.ZERO, normalizedReferenceId, TransactionStatus.DUPLICATE,
                    "Duplicate transaction reference");
            globalFailedLog.add(duplicate);
            throw new DuplicateTransactionException(normalizedReferenceId);
        }
        return normalizedReferenceId;
    }

    private Customer findRecipient(String recipientPhone, CustomerService customerService) {
        if (customerService == null) {
            throw new AccountNotFoundException("customer service");
        }
        String normalizedPhone = AuthService.validatePhone(recipientPhone);
        return customerService.findByPhone(normalizedPhone)
                .orElseThrow(() -> new AccountNotFoundException(normalizedPhone));
    }

    private Account resolveRecipientAccount(Account sender, Customer recipient) {
        boolean sameOwner = sender.getCustomerId().equals(recipient.getCustomerId());
        AccountType targetType = sameOwner && sender.getAccountType() == AccountType.WALLET
                ? AccountType.SAVINGS
                : AccountType.WALLET;
        Optional<Account> account = recipient.getAccountByType(targetType);
        return account.orElseThrow(() -> new AccountNotFoundException(
                recipient.getPhoneNumber() + " " + targetType.name().toLowerCase() + " account"
        ));
    }

    private BigDecimal calculateTransferFee(Account sender, Customer recipient, BigDecimal amount) {
        if (sender.getCustomerId().equals(recipient.getCustomerId())) {
            return BigDecimal.ZERO;
        }
        return WalletAccount.calculateTransferFee(amount);
    }

    private void ensureSavingsWithdrawalLimit(UUID accountId) {
        if (getDailyWithdrawals(accountId, LocalDate.now()).size() >= SavingsAccount.DAILY_WITHDRAWAL_LIMIT) {
            throw new WithdrawalLimitExceededException(SavingsAccount.DAILY_WITHDRAWAL_LIMIT);
        }
    }

    private void ensureSufficientBalance(Account account, BigDecimal required) {
        if (account.getBalance().compareTo(required) < 0) {
            throw new InsufficientBalanceException(required, account.getBalance());
        }
    }

    private void requireAccount(Account account) {
        if (account == null) {
            throw new AccountNotFoundException("account");
        }
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        return normalizedAmount;
    }

    private void markProcessed(String referenceId) {
        processedReferenceIds.add(referenceId);
    }

    private void logSuccess(Transaction transaction) {
        historyByAccountId
                .computeIfAbsent(transaction.getAccountId(), key -> new ArrayList<>())
                .add(transaction);
    }

    private void logFailed(Account account, UUID targetAccountId, TransactionType transactionType,
                           BigDecimal amount, BigDecimal fee, String referenceId, String description) {
        globalFailedLog.add(createStatusTransaction(account, targetAccountId, transactionType, amount, fee,
                referenceId, TransactionStatus.FAILED, description));
    }

    private Transaction createStatusTransaction(Account account, UUID targetAccountId, TransactionType transactionType,
                                                BigDecimal amount, BigDecimal fee, String referenceId,
                                                TransactionStatus status, String description) {
        return new Transaction(
                UUID.randomUUID(),
                referenceId,
                account == null ? null : account.getAccountId(),
                targetAccountId,
                transactionType,
                amount == null ? BigDecimal.ZERO : amount,
                fee == null ? BigDecimal.ZERO : fee,
                status,
                LocalDateTime.now(),
                description
        );
    }

    private String normalizeReference(String referenceId) {
        return referenceId == null ? "" : referenceId.trim();
    }
}
