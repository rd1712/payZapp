CREATE TABLE payments (
                          payment_id      UUID           PRIMARY KEY,
                          from_wallet_id  UUID           NOT NULL,
                          to_wallet_id    UUID           NOT NULL,
                          amount          DECIMAL(19, 4) NOT NULL,
                          status          VARCHAR(50)    NOT NULL,
                          idempotency_key VARCHAR(255)   NOT NULL UNIQUE,
                          created_at      TIMESTAMP      NOT NULL,
                          updated_at      TIMESTAMP      NOT NULL
);