package com.igirepay;

import com.igirepay.lab1.exceptions.AccountLockedException;
import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.DuplicateTransactionException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidAmountException;
import com.igirepay.lab1.exceptions.InvalidPhoneNumberException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.exceptions.InvalidPinFormatException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.service.AccountService;
import com.igirepay.lab1.service.AuthService;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab1.service.TransactionService;
import com.igirepay.lab2.service.ReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private static final CustomerService customerService = new CustomerService();
    private static final AuthService authService = new AuthService(customerService);
    private static final AccountService accountService = new AccountService();
    private static final TransactionService transactionService = new TransactionService();
    private static final ReportService reportService = new ReportService();

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Customer currentCustomer = null;
            boolean running = true;
            while (running) {
                try {
                    if (currentCustomer == null) {
                        currentCustomer = handleAuthMenu(scanner);
                        if (currentCustomer == null) {
                            running = false;
                        }
                    } else {
                        MenuResult result = handleCustomerMenu(scanner, currentCustomer);
                        currentCustomer = result.customer();
                        running = !result.exitRequested();
                    }
                } catch (RuntimeException exception) {
                    System.out.println(toUserMessage(exception));
                }
            }
        }
    }

    private static Customer handleAuthMenu(Scanner scanner) {
        System.out.println();
        System.out.println("--- IgirePay Payment Gateway ---");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("0. Exit");

        int choice = readMenuChoice(scanner, "Choose an option: ");
        return switch (choice) {
            case 1 -> register(scanner);
            case 2 -> login(scanner);
            case 0 -> {
                System.out.println("Goodbye!");
                yield null;
            }
            default -> {
                System.out.println("Invalid option, please try again.");
                yield handleAuthMenu(scanner);
            }
        };
    }

    private static MenuResult handleCustomerMenu(Scanner scanner, Customer customer) {
        printCustomerMenu(customer);
        int choice = readMenuChoice(scanner, "Choose an option: ");

        switch (choice) {
            case 1 -> checkBalance(scanner, customer);
            case 2 -> {
                depositMoney(scanner, customer);
                customer = customerService.refresh(customer);
            }
            case 3 -> {
                withdrawMoney(scanner, customer);
                customer = customerService.refresh(customer);
            }
            case 4 -> {
                transferMoney(scanner, customer);
                customer = customerService.refresh(customer);
            }
            case 5 -> viewTransactionHistory(scanner, customer);
            case 6 -> {
                createAccount(scanner, customer);
                customer = customerService.refresh(customer);
            }
            case 7 -> {
                changePin(scanner, customer);
                customer = customerService.refresh(customer);
            }
            case 8 -> exportTransactionHistory(scanner, customer);
            case 9 -> viewDailySummary(scanner, customer);
            case 10 -> reportService.printFullStatement(customer.getCustomerId());
            case 11 -> {
                System.out.println("Logged out.");
                return new MenuResult(null, false);
            }
            case 0 -> {
                System.out.println("Goodbye!");
                return new MenuResult(customer, true);
            }
            default -> System.out.println("Invalid option, please try again.");
        }
        return new MenuResult(customer, false);
    }

    private static void printCustomerMenu(Customer customer) {
        System.out.println();
        System.out.println("--- Welcome, " + customer.getFullName() + " ---");
        System.out.println("1. Check balance");
        System.out.println("2. Deposit money");
        System.out.println("3. Withdraw money");
        System.out.println("4. Transfer money");
        System.out.println("5. View transaction history");
        System.out.println("6. Create savings / wallet account");
        System.out.println("7. Change PIN");
        System.out.println("8. Export transaction history to CSV");
        System.out.println("9. View daily summary");
        System.out.println("10. View full account statement");
        System.out.println("11. Logout");
        System.out.println("0. Exit");
    }

    private static Customer register(Scanner scanner) {
        System.out.print("Full name: ");
        String fullName = scanner.nextLine();
        System.out.print("Phone number: ");
        String phone = scanner.nextLine();
        System.out.print("Create 5-digit PIN: ");
        String pin = scanner.nextLine();
        System.out.print("Confirm PIN: ");
        String confirmPin = scanner.nextLine();

        if (!pin.equals(confirmPin)) {
            throw new InvalidPinFormatException("PIN confirmation does not match.");
        }

        Customer customer = authService.register(fullName, phone, pin);
        System.out.println("Registration successful. You are now logged in.");
        return customer;
    }

    private static Customer login(Scanner scanner) {
        System.out.print("Phone number: ");
        String phone = scanner.nextLine();
        System.out.print("PIN: ");
        String pin = scanner.nextLine();

        Customer customer = authService.login(phone, pin);
        System.out.println("Login successful.");
        return customer;
    }

    private static void checkBalance(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account != null) {
            System.out.println(account.getAccountType() + " balance: " +
                    formatMoney(accountService.getBalance(account.getAccountId(), customer)));
        }
    }

    private static void depositMoney(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) return;

        BigDecimal amount = readAmount(scanner);
        String referenceId = UUID.randomUUID().toString();
        transactionService.deposit(account, amount, referenceId);

        System.out.println("Deposit successful. Reference ID: " + referenceId);
        System.out.println("New balance: " + formatMoney(account.getBalance()));
    }

    private static void withdrawMoney(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) return;

        BigDecimal amount = readAmount(scanner);
        System.out.print("Confirm with PIN: ");
        String pin = scanner.nextLine();
        String referenceId = UUID.randomUUID().toString();
        transactionService.withdraw(account, amount, pin, referenceId);

        System.out.println("Withdrawal successful. Reference ID: " + referenceId);
        System.out.println("New balance: " + formatMoney(account.getBalance()));
    }

    private static void transferMoney(Scanner scanner, Customer customer) {
        Account sender = selectAccount(scanner, customer);
        if (sender == null) return;

        BigDecimal amount = readAmount(scanner);
        System.out.print("Recipient phone number: ");
        String recipientPhone = scanner.nextLine();
        String recipientName = transactionService.lookupRecipientName(recipientPhone, customerService);
        System.out.println("Sending to: " + recipientName + " - confirm with your PIN");
        System.out.print("PIN: ");
        String pin = scanner.nextLine();

        transactionService.validateAccountPin(sender, pin);
        BigDecimal fee = transactionService.previewTransferFee(sender, recipientPhone, amount, customerService);
        System.out.println("Fee: " + formatMoney(fee) + ". Total deducted: " + formatMoney(amount.add(fee)));

        String referenceId = UUID.randomUUID().toString();
        List<Transaction> transactions = transactionService.transfer(sender, recipientPhone, amount, pin,
                referenceId, customerService);

        System.out.println("Transfer successful. Reference ID: " + referenceId);
        System.out.println("Transactions created: " + transactions.size());
        System.out.println("New balance: " + formatMoney(sender.getBalance()));
    }

    private static void viewTransactionHistory(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) return;

        List<Transaction> history = transactionService.getHistory(account.getAccountId());
        if (history.isEmpty()) {
            System.out.println("No transactions found for this account.");
            return;
        }

        System.out.println("Transaction history for " + account.getAccountType() + ":");
        for (Transaction transaction : history) {
            System.out.println(formatTransaction(transaction));
        }
    }

    private static void createAccount(Scanner scanner, Customer customer) {
        System.out.println("1. Create wallet account");
        System.out.println("2. Create savings account");
        int choice = readMenuChoice(scanner, "Choose account type: ");

        switch (choice) {
            case 1 -> {
                accountService.createWallet(customer);
                System.out.println("Wallet account created.");
            }
            case 2 -> {
                accountService.createSavings(customer);
                System.out.println("Savings account created.");
            }
            default -> System.out.println("Invalid option, please try again.");
        }
    }

    private static void changePin(Scanner scanner, Customer customer) {
        System.out.print("Current PIN: ");
        String currentPin = scanner.nextLine();
        if (!customer.validatePin(currentPin)) {
            throw new InvalidPinException(0);
        }

        System.out.print("New 5-digit PIN: ");
        String newPin = scanner.nextLine();
        System.out.print("Confirm new PIN: ");
        String confirmPin = scanner.nextLine();
        if (!newPin.equals(confirmPin)) {
            throw new InvalidPinFormatException("PIN confirmation does not match.");
        }

        AuthService.validatePinFormat(newPin);
        customer.setHashedPin(AuthService.hashPin(newPin));
        customer.resetFailedAttempts();
        customerService.update(customer);
        System.out.println("PIN changed successfully.");
    }

    private static void exportTransactionHistory(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) return;

        LocalDate from = readDate(scanner, "From date (yyyy-MM-dd): ");
        LocalDate to = readDate(scanner, "To date (yyyy-MM-dd): ");
        System.out.print("CSV file path: ");
        String filePath = scanner.nextLine().trim();
        reportService.exportToCSV(account.getAccountId(), from, to, filePath);
        System.out.println("Transaction history exported to " + filePath + ".");
    }

    private static void viewDailySummary(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) return;

        LocalDate date = readDate(scanner, "Summary date (yyyy-MM-dd): ");
        reportService.printDailySummary(account.getAccountId(), date);
    }

    private static Account selectAccount(Scanner scanner, Customer customer) {
        Optional<Account> wallet = customer.getAccountByType(AccountType.WALLET);
        Optional<Account> savings = customer.getAccountByType(AccountType.SAVINGS);

        if (wallet.isEmpty() && savings.isEmpty()) {
            System.out.println("No account exists yet. Use option 6 to create one.");
            return null;
        }

        System.out.println("Select account:");
        wallet.ifPresent(a -> System.out.println("1. Wallet - " + formatMoney(a.getBalance())));
        savings.ifPresent(a -> System.out.println("2. Savings - " + formatMoney(a.getBalance())));
        int choice = readMenuChoice(scanner, "Choose account: ");

        if (choice == 1 && wallet.isPresent()) return wallet.get();
        if (choice == 2 && savings.isPresent()) return savings.get();

        System.out.println("Selected account does not exist.");
        return null;
    }

    private static int readMenuChoice(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Invalid input, please enter a menu number.");
            }
        }
    }

    private static BigDecimal readAmount(Scanner scanner) {
        while (true) {
            System.out.print("Amount: ");
            String input = scanner.nextLine().trim().replace(",", "");
            try {
                return new BigDecimal(input).stripTrailingZeros();
            } catch (NumberFormatException exception) {
                System.out.println("Invalid amount, please enter a numeric value.");
            }
        }
    }

    private static LocalDate readDate(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException exception) {
                System.out.println("Invalid date, please use yyyy-MM-dd.");
            }
        }
    }

    private static String formatMoney(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return MONEY_FORMAT.format(safe.setScale(2, RoundingMode.HALF_UP)) + " RWF";
    }

    private static String formatTransaction(Transaction transaction) {
        String fee = transaction.getFee().compareTo(BigDecimal.ZERO) > 0
                ? ", fee " + formatMoney(transaction.getFee())
                : "";
        return transaction.getTimestamp().toString() + " | " +
                transaction.getTransactionType() + " | " +
                formatMoney(transaction.getAmount()) + fee + " | " +
                transaction.getStatus() + " | Ref: " + transaction.getReferenceId();
    }

    private static String toUserMessage(RuntimeException exception) {
        if (exception instanceof DuplicateTransactionException duplicate) {
            return "Duplicate transaction. Reference ID already processed: " + duplicate.getReferenceId();
        }
        if (exception instanceof InsufficientBalanceException insufficient) {
            return "Insufficient balance. Required " + formatMoney(insufficient.getRequired()) +
                    ", available " + formatMoney(insufficient.getAvailable()) + ".";
        }
        if (exception instanceof InvalidPinException invalidPin) {
            return "Invalid PIN. Attempts remaining: " + invalidPin.getAttemptsRemaining() + ".";
        }
        if (exception instanceof AccountLockedException locked) {
            return "Account locked for phone number " + locked.getPhoneNumber() + ".";
        }
        if (exception instanceof InvalidPhoneNumberException) {
            return "Phone number must be exactly 10 numeric digits.";
        }
        if (exception instanceof InvalidPinFormatException pinFormat) {
            return pinFormat.getMessage();
        }
        if (exception instanceof AccountNotFoundException notFound) {
            return "Not found: " + notFound.getIdentifier() + ".";
        }
        if (exception instanceof InvalidAmountException amountException) {
            return amountException.getMessage();
        }
        if (exception instanceof DatabaseException databaseException) {
            String cause = databaseException.getCause() != null ? " (" + databaseException.getCause().getMessage() + ")" : "";
            return "Database error: " + databaseException.getMessage() + cause + ".";
        }
        return exception.getMessage() == null ? "Something went wrong. Please try again." : exception.getMessage();
    }

    private record MenuResult(Customer customer, boolean exitRequested) {
    }
}
