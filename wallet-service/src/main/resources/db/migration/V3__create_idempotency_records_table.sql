-- V3__create_idempotency_records_table.sql
CREATE TABLE idempotency_records
(
    idempotency_key VARCHAR(255) PRIMARY KEY,
    status          VARCHAR(50) NOT NULL,
    response        TEXT,
    created_at      TIMESTAMP   NOT NULL
);