CREATE TABLE IF NOT EXISTS users
(
    id         SERIAL PRIMARY KEY,

    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP    NULL     DEFAULT NULL,

    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(50)  NOT NULL
);
