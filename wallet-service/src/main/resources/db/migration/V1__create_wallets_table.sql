CREATE TABLE wallets
(
    wallet_id  UUID           PRIMARY KEY,
    user_id    UUID           NOT NULL,
    status     VARCHAR(50)    NOT NULL,
    balance    DECIMAL(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP      NOT NULL
);