ALTER TABLE customers
    DROP CONSTRAINT uk_customers_document;

ALTER TABLE customers
    ADD CONSTRAINT uk_customers_document_number UNIQUE (document_number);
