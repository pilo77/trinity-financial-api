ALTER TABLE financial_transactions
    ADD COLUMN transaction_date TIMESTAMP;

UPDATE financial_transactions
SET transaction_date = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE transaction_date IS NULL;

ALTER TABLE financial_transactions
    ALTER COLUMN transaction_date SET NOT NULL;

CREATE INDEX idx_transactions_transaction_date
    ON financial_transactions (transaction_date);
