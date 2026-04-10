CREATE TABLE users
(
    user_id       UUID PRIMARY KEY,
    first_name    VARCHAR(255) not null,
    last_name     VARCHAR(255) not null,
    email       VARCHAR(255) not null unique,
    password_hash VARCHAR(255) not null,
    phone_number  VARCHAR(20)  not null unique,
    status        VARCHAR(50)  not null,
    created_at    TIMESTAMP    not null,
    updated_at    TIMESTAMP    not null
);