CREATE TABLE ledger_entries
(
    entry_id       UUID           PRIMARY KEY,
    wallet_id      UUID           NOT NULL,
    transaction_id UUID           NOT NULL,
    type           VARCHAR(10)    NOT NULL,
    amount         DECIMAL(19, 4) NOT NULL,
    description    VARCHAR(255)   NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);