package com.igirepay;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountLockedException;
import com.igirepay.lab1.model.AccountNotFoundException;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.DuplicateTransactionException;
import com.igirepay.lab1.model.InsufficientBalanceException;
import com.igirepay.lab1.model.InvalidAmountException;
import com.igirepay.lab1.model.InvalidPhoneNumberException;
import com.igirepay.lab1.model.InvalidPinException;
import com.igirepay.lab1.model.InvalidPinFormatException;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.service.AccountService;
import com.igirepay.lab1.service.AuthService;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab1.service.TransactionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final CustomerService CUSTOMER_SERVICE = new CustomerService();
    private static final AuthService AUTH_SERVICE = new AuthService(CUSTOMER_SERVICE);
    private static final AccountService ACCOUNT_SERVICE = new AccountService();
    private static final TransactionService TRANSACTION_SERVICE = new TransactionService();
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter HISTORY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Customer currentCustomer = null;
            boolean running = true;

            while (running) {
                try {
                    if (currentCustomer == null) {
                        currentCustomer = handleAuthMenu(scanner);
                        if (currentCustomer == null && readLastChoiceWasExit()) {
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

    private static boolean lastChoiceWasExit;

    private static Customer handleAuthMenu(Scanner scanner) {
        lastChoiceWasExit = false;
        System.out.println();
        System.out.println("--- IgirePay Payment Gateway (Lab 1) ---");
        System.out.println("1. Register as a new user");
        System.out.println("2. Login");
        System.out.println("0. Exit");

        int choice = readMenuChoice(scanner, "Choose an option: ");
        return switch (choice) {
            case 1 -> register(scanner);
            case 2 -> login(scanner);
            case 0 -> {
                lastChoiceWasExit = true;
                System.out.println("Goodbye!");
                yield null;
            }
            default -> {
                System.out.println("Invalid option, please try again.");
                yield null;
            }
        };
    }

    private static boolean readLastChoiceWasExit() {
        return lastChoiceWasExit;
    }

    private static MenuResult handleCustomerMenu(Scanner scanner, Customer customer) {
        printCustomerMenu(customer);
        int choice = readMenuChoice(scanner, "Choose an option: ");

        switch (choice) {
            case 1 -> checkBalance(scanner, customer);
            case 2 -> depositMoney(scanner, customer);
            case 3 -> withdrawMoney(scanner, customer);
            case 4 -> transferMoney(scanner, customer);
            case 5 -> viewTransactionHistory(scanner, customer);
            case 6 -> createAccount(scanner, customer);
            case 7 -> changePin(scanner, customer);
            case 8 -> {
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
        System.out.println("6. Create savings account / wallet account");
        System.out.println("7. Change PIN");
        System.out.println("8. Logout");
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

        Customer customer = AUTH_SERVICE.register(fullName, phone, pin);
        System.out.println("Registration successful. You are now logged in.");
        return customer;
    }

    private static Customer login(Scanner scanner) {
        System.out.print("Phone number: ");
        String phone = scanner.nextLine();
        System.out.print("PIN: ");
        String pin = scanner.nextLine();

        Customer customer = AUTH_SERVICE.login(phone, pin);
        System.out.println("Login successful.");
        return customer;
    }

    private static void checkBalance(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account != null) {
            System.out.println(account.getAccountType() + " balance: " + formatMoney(
                    ACCOUNT_SERVICE.getBalance(account.getAccountId(), customer)));
        }
    }

    private static void depositMoney(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner);
        String referenceId = newReferenceId();
        TRANSACTION_SERVICE.deposit(account, amount, referenceId);

        System.out.println("Deposit successful. Reference ID: " + referenceId);
        System.out.println("New balance: " + formatMoney(account.getBalance()));
    }

    private static void withdrawMoney(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner);
        System.out.print("Confirm with PIN: ");
        String pin = scanner.nextLine();
        String referenceId = newReferenceId();
        TRANSACTION_SERVICE.withdraw(account, amount, pin, referenceId);

        System.out.println("Withdrawal successful. Reference ID: " + referenceId);
        System.out.println("New balance: " + formatMoney(account.getBalance()));
    }

    private static void transferMoney(Scanner scanner, Customer customer) {
        Account sender = selectAccount(scanner, customer);
        if (sender == null) {
            return;
        }

        BigDecimal amount = readAmount(scanner);
        System.out.print("Recipient phone number: ");
        String recipientPhone = scanner.nextLine();
        String recipientName = TRANSACTION_SERVICE.lookupRecipientName(recipientPhone, CUSTOMER_SERVICE);
        System.out.println("Sending to: " + recipientName + " - confirm with your PIN");
        System.out.print("PIN: ");
        String pin = scanner.nextLine();

        TRANSACTION_SERVICE.validateAccountPin(sender, pin);
        BigDecimal fee = TRANSACTION_SERVICE.previewTransferFee(sender, recipientPhone, amount, CUSTOMER_SERVICE);
        BigDecimal totalDeducted = amount.add(fee);
        System.out.println("Fee: " + formatMoney(fee) + ". Total deducted: " + formatMoney(totalDeducted));

        String referenceId = newReferenceId();
        List<Transaction> transactions = TRANSACTION_SERVICE.transfer(
                sender,
                recipientPhone,
                amount,
                pin,
                referenceId,
                CUSTOMER_SERVICE
        );

        System.out.println("Transfer successful. Reference ID: " + referenceId);
        System.out.println("Transactions created: " + transactions.size());
        System.out.println("New balance: " + formatMoney(sender.getBalance()));
    }

    private static void viewTransactionHistory(Scanner scanner, Customer customer) {
        Account account = selectAccount(scanner, customer);
        if (account == null) {
            return;
        }

        List<Transaction> history = TRANSACTION_SERVICE.getHistory(account.getAccountId());
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
                ACCOUNT_SERVICE.createWallet(customer);
                System.out.println("Wallet account created.");
            }
            case 2 -> {
                ACCOUNT_SERVICE.createSavings(customer);
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
        System.out.println("PIN changed successfully.");
    }

    private static Account selectAccount(Scanner scanner, Customer customer) {
        Optional<Account> wallet = customer.getAccountByType(AccountType.WALLET);
        Optional<Account> savings = customer.getAccountByType(AccountType.SAVINGS);

        if (wallet.isEmpty() && savings.isEmpty()) {
            System.out.println("No account exists yet. Use option 6 to create one.");
            return null;
        }

        System.out.println("Select account:");
        wallet.ifPresent(account -> System.out.println("1. Wallet - " + formatMoney(account.getBalance())));
        savings.ifPresent(account -> System.out.println("2. Savings - " + formatMoney(account.getBalance())));
        int choice = readMenuChoice(scanner, "Choose account: ");

        if (choice == 1 && wallet.isPresent()) {
            return wallet.get();
        }
        if (choice == 2 && savings.isPresent()) {
            return savings.get();
        }

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

    private static String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return MONEY_FORMAT.format(safeAmount.setScale(2, RoundingMode.HALF_UP)) + " RWF";
    }

    private static String formatTransaction(Transaction transaction) {
        String fee = transaction.getFee().compareTo(BigDecimal.ZERO) > 0
                ? ", fee " + formatMoney(transaction.getFee())
                : "";
        return transaction.getTimestamp().format(HISTORY_TIME_FORMAT) + " | " +
                transaction.getTransactionType() + " | " +
                formatMoney(transaction.getAmount()) + fee + " | " +
                transaction.getStatus() + " | Ref: " +
                transaction.getReferenceId();
    }

    private static String newReferenceId() {
        return UUID.randomUUID().toString();
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
        return exception.getMessage() == null ? "Something went wrong. Please try again." : exception.getMessage();
    }

    private record MenuResult(Customer customer, boolean exitRequested) {
    }
}
