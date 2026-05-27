# IgirePay Wallet System

A secure desktop digital wallet system built in Java across three labs.

---

## How to Run (Lab 3 — JavaFX GUI)

```
mvn clean javafx:run
```

Or run `com.igirepay.lab3.ui.Launcher` directly from your IDE.

---

## Database Setup (Fresh Install)

1. Create a PostgreSQL database named `igirepay`
2. Run the following SQL to create all tables:

```sql
CREATE TABLE customers (
    customer_id  SERIAL          PRIMARY KEY,
    full_name    VARCHAR(255)    NOT NULL,
    phone_number VARCHAR(10)     NOT NULL UNIQUE,
    pin          VARCHAR(64)     NOT NULL,
    created_at   TIMESTAMP       NOT NULL
);

CREATE TABLE accounts (
    account_id   SERIAL          PRIMARY KEY,
    customer_id  INTEGER         NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
    account_type VARCHAR(10)     NOT NULL CHECK (account_type IN ('WALLET', 'SAVINGS')),
    account_name VARCHAR(100)    NOT NULL DEFAULT 'My Account',
    balance      NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    created_at   TIMESTAMP       NOT NULL,
    is_active    BOOLEAN         NOT NULL DEFAULT TRUE,
    pin          VARCHAR(64)     NOT NULL
);

CREATE TABLE transactions (
    transaction_id    SERIAL          PRIMARY KEY,
    reference_id      VARCHAR(255)    NOT NULL,
    account_id        INTEGER         NOT NULL REFERENCES accounts(account_id),
    target_account_id INTEGER         REFERENCES accounts(account_id),
    transaction_type  VARCHAR(20)     NOT NULL CHECK (transaction_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT','FEE')),
    amount            NUMERIC(19, 4)  NOT NULL,
    fee               NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    status            VARCHAR(20)     NOT NULL CHECK (status IN ('SUCCESS','FAILED','DUPLICATE')),
    timestamp         TIMESTAMP       NOT NULL,
    description       VARCHAR(500)    NOT NULL
);
```

3. Update credentials in `DBConnection.java` if needed (default: user=`postgres`, password=`Mbabazi12`)

---

## Database Migration (Existing Database → New Schema)

If you already have the old schema (UUID primary keys, `is_locked`, `failed_attempts`, `processed_requests`), run these migration queries **in order**:

```sql
-- Step 1: Drop the processed_requests table (no longer used)
DROP TABLE IF EXISTS processed_requests;

-- Step 2: Remove is_locked and failed_attempts from customers
ALTER TABLE customers DROP COLUMN IF EXISTS is_locked;
ALTER TABLE customers DROP COLUMN IF EXISTS failed_attempts;

-- Step 3: Add account_name column to accounts (if not already present)
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS account_name VARCHAR(100) NOT NULL DEFAULT 'My Account';

-- Step 4: Migrate customers UUID → SERIAL INT
-- (Run only if customer_id is currently UUID type)
ALTER TABLE accounts DROP CONSTRAINT IF EXISTS accounts_customer_id_fkey;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_account_id_fkey;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_target_account_id_fkey;

CREATE SEQUENCE IF NOT EXISTS customers_customer_id_seq;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS customer_id_new SERIAL;
UPDATE customers SET customer_id_new = nextval('customers_customer_id_seq');

-- Propagate new int IDs to accounts
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS customer_id_new INTEGER;
UPDATE accounts a
SET customer_id_new = c.customer_id_new
FROM customers c
WHERE a.customer_id::text = c.customer_id::text;

-- Step 5: Migrate accounts UUID → SERIAL INT
CREATE SEQUENCE IF NOT EXISTS accounts_account_id_seq;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS account_id_new SERIAL;
UPDATE accounts SET account_id_new = nextval('accounts_account_id_seq');

-- Propagate new account int IDs to transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS account_id_new INTEGER;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS target_account_id_new INTEGER;
UPDATE transactions t
SET account_id_new = a.account_id_new
FROM accounts a
WHERE t.account_id::text = a.account_id::text;
UPDATE transactions t
SET target_account_id_new = a.account_id_new
FROM accounts a
WHERE t.target_account_id::text = a.account_id::text;

-- Step 6: Migrate transactions UUID → SERIAL INT
CREATE SEQUENCE IF NOT EXISTS transactions_transaction_id_seq;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS transaction_id_new SERIAL;

-- Step 7: Swap columns — customers
ALTER TABLE customers DROP CONSTRAINT customers_pkey;
ALTER TABLE customers DROP COLUMN customer_id;
ALTER TABLE customers RENAME COLUMN customer_id_new TO customer_id;
ALTER TABLE customers ADD PRIMARY KEY (customer_id);

-- Step 8: Swap columns — accounts
ALTER TABLE accounts DROP CONSTRAINT accounts_pkey;
ALTER TABLE accounts DROP COLUMN account_id;
ALTER TABLE accounts DROP COLUMN customer_id;
ALTER TABLE accounts RENAME COLUMN account_id_new TO account_id;
ALTER TABLE accounts RENAME COLUMN customer_id_new TO customer_id;
ALTER TABLE accounts ADD PRIMARY KEY (account_id);
ALTER TABLE accounts ADD CONSTRAINT accounts_customer_id_fkey
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE;

-- Step 9: Swap columns — transactions
ALTER TABLE transactions DROP CONSTRAINT transactions_pkey;
ALTER TABLE transactions DROP COLUMN transaction_id;
ALTER TABLE transactions DROP COLUMN account_id;
ALTER TABLE transactions DROP COLUMN target_account_id;
ALTER TABLE transactions RENAME COLUMN transaction_id_new TO transaction_id;
ALTER TABLE transactions RENAME COLUMN account_id_new TO account_id;
ALTER TABLE transactions RENAME COLUMN target_account_id_new TO target_account_id;
ALTER TABLE transactions ADD PRIMARY KEY (transaction_id);
ALTER TABLE transactions ADD CONSTRAINT transactions_account_id_fkey
    FOREIGN KEY (account_id) REFERENCES accounts(account_id);
ALTER TABLE transactions ADD CONSTRAINT transactions_target_account_id_fkey
    FOREIGN KEY (target_account_id) REFERENCES accounts(account_id);
```

> **Note:** After migration, update `DBConnection.java`, `CustomerDAO`, `AccountDAO`, and `TransactionDAO`
> to use `INTEGER` / `int` instead of `UUID` for all ID columns.

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
│   ├── dao/          — CustomerDAO, AccountDAO, TransactionDAO
│   └── service/      — ReportService
├── lab3/
│   ├── ui/           — MainApp, Launcher, SceneManager, SessionManager
│   ├── controller/   — One controller per screen
│   └── util/         — CsvExporter
└── Main.java         — Original console entry point (Lab 1/2)

src/main/resources/
├── fxml/             — login, dashboard, customer, account, deposit, withdraw, transfer,
│                       savings, history, reports, changePin
└── css/              — style.css (reserved for future styling)
```

---

## Screens

| Screen       | Description                                                                 |
|---|---|
| Login        | Phone + PIN login                                                           |
| Dashboard    | Account overview + navigation to all features                               |
| Customer     | Register new customer or update profile                                     |
| Account      | Create wallet/savings accounts, deactivate accounts                         |
| Deposit      | Add funds to any account                                                    |
| Withdraw     | Withdraw from any account (savings: max 3/day)                              |
| Transfer     | Send to another customer's wallet (1% fee, min 10 RWF, max 500 RWF)        |
| Savings      | Move money between own wallet and savings (no fee, no external transfers)   |
| History      | View transaction history per account                                        |
| Reports      | Daily summary, CSV export, full statement with running balance               |
| Change PIN   | Update PIN with current PIN verification                                    |

---

## Transfer Fee (MTN-style)

| Amount        | Fee                  |
|---|---|
| Any amount    | 1% of transfer amount |
| Minimum fee   | 10 RWF               |
| Maximum fee   | 500 RWF              |

Fee is deducted from the sender's wallet in addition to the transfer amount.
Own wallet → own savings moves are **free**.
