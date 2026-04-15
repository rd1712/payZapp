CREATE TABLE transaction_records (
                                     payment_id    UUID           PRIMARY KEY,
                                     from_wallet_id UUID          NOT NULL,
                                     to_wallet_id  UUID           NOT NULL,
                                     amount        DECIMAL(19, 4) NOT NULL,
                                     status        VARCHAR(50)    NOT NULL,
                                     settled_at    TIMESTAMP      NOT NULL,
                                     recorded_at   TIMESTAMP      NOT NULL
);