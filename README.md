# IgirePay Wallet System

A secure desktop digital wallet system built in Java across three labs.

---

## How to Run (Lab 3 — JavaFX GUI)

```
mvn clean javafx:run
```

Or run `com.igirepay.lab3.ui.Launcher` directly from your IDE.

---

## Database Setup

1. Create a PostgreSQL database named `igirepay`
2. Run the following SQL:

```sql
CREATE TABLE customers (
    customer_id     UUID            PRIMARY KEY,
    full_name       VARCHAR(255)    NOT NULL,
    phone_number    VARCHAR(10)     NOT NULL UNIQUE,
    pin             VARCHAR(5)      NOT NULL,
    is_locked       BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL
);

CREATE TABLE accounts (
    account_id   UUID           PRIMARY KEY,
    customer_id  UUID           NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
    account_type VARCHAR(10)    NOT NULL CHECK (account_type IN ('WALLET', 'SAVINGS')),
    balance      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL,
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    pin          VARCHAR(5)     NOT NULL
);

CREATE TABLE transactions (
    transaction_id    UUID           PRIMARY KEY,
    reference_id      VARCHAR(255)   NOT NULL,
    account_id        UUID           NOT NULL REFERENCES accounts(account_id),
    target_account_id UUID           REFERENCES accounts(account_id),
    transaction_type  VARCHAR(20)    NOT NULL CHECK (transaction_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT','FEE')),
    amount            NUMERIC(19, 4) NOT NULL,
    fee               NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status            VARCHAR(20)    NOT NULL CHECK (status IN ('SUCCESS','FAILED','DUPLICATE')),
    timestamp         TIMESTAMP      NOT NULL,
    description       VARCHAR(500)   NOT NULL
);

CREATE TABLE processed_requests (
    reference_id VARCHAR(255) PRIMARY KEY
);
```

3. Update credentials in `DBConnection.java` if needed (default: user=`postgres`, password=`Mbabazi12`)

---

## Project Structure

```
src/main/java/com/igirepay/
├── lab1/
│   ├── model/        — Account, WalletAccount, SavingsAccount, Customer, Transaction, enums
│   ├── service/      — AuthService, CustomerService, AccountService, TransactionService
│   └── exceptions/   — All custom exceptions
├── lab2/
│   ├── config/       — DBConnection (JDBC)
│   ├── dao/          — CustomerDAO, AccountDAO, TransactionDAO, ProcessedRequestDAO
│   └── service/      — ReportService
├── lab3/
│   ├── ui/           — MainApp, Launcher, SceneManager, SessionManager
│   ├── controller/   — One controller per screen
│   └── util/         — CsvExporter
└── Main.java         — Original console entry point (Lab 1/2)

src/main/resources/
├── fxml/             — login, dashboard, customer, account, transaction, history, reports, changePin
└── css/              — style.css (reserved for future styling)
```

---

## Screens

| Screen | Description |
|---|---|
| Login | Phone + PIN login with lockout after 3 failures |
| Dashboard | Account overview + navigation |
| Customer | Register new customer or update profile |
| Account | Create wallet/savings accounts, deactivate accounts |
| Transaction | Deposit, withdraw, transfer |
| History | View transaction history per account |
| Reports | Daily summary, CSV export, full statement with running balance |
| Change PIN | Update PIN with current PIN verification |
