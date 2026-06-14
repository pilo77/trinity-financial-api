CREATE TABLE customers (
    id UUID PRIMARY KEY,
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(30) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(30),
    birth_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_customers_document UNIQUE (document_type, document_number),
    CONSTRAINT uk_customers_email UNIQUE (email)
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number CHAR(10) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    gmf_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    customer_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_accounts_number_format
        CHECK (
            account_number ~ '^[0-9]{10}$'
            AND (
                (account_type = 'SAVINGS' AND account_number LIKE '53%')
                OR (account_type = 'CHECKING' AND account_number LIKE '33%')
            )
        ),
    CONSTRAINT ck_accounts_type CHECK (account_type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELLED')),
    CONSTRAINT ck_accounts_balance CHECK (balance >= 0),
    CONSTRAINT ck_accounts_available_balance CHECK (available_balance >= 0)
);

CREATE TABLE financial_transactions (
    id UUID PRIMARY KEY,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    source_account_id UUID,
    destination_account_id UUID,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_transactions_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts (id),
    CONSTRAINT ck_transactions_type
        CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT ck_transactions_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_different_accounts
        CHECK (
            source_account_id IS NULL
            OR destination_account_id IS NULL
            OR source_account_id <> destination_account_id
        )
);

CREATE TABLE account_movements (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    financial_transaction_id UUID NOT NULL,
    movement_type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    balance_after_movement NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_movements_account
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_movements_transaction
        FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions (id),
    CONSTRAINT ck_movements_type CHECK (movement_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_movements_amount CHECK (amount > 0),
    CONSTRAINT ck_movements_balance CHECK (balance_after_movement >= 0)
);

CREATE INDEX idx_accounts_customer ON accounts (customer_id);
CREATE INDEX idx_transactions_source_account ON financial_transactions (source_account_id);
CREATE INDEX idx_transactions_destination_account ON financial_transactions (destination_account_id);
CREATE INDEX idx_transactions_created_at ON financial_transactions (created_at);
CREATE INDEX idx_movements_account_created_at ON account_movements (account_id, created_at);
CREATE INDEX idx_movements_transaction ON account_movements (financial_transaction_id);
