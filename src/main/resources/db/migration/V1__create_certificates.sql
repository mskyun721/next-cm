-- Certificate Manager: certificates table
-- NOTE: this project currently bootstraps schema via src/main/resources/db/schema.sql
--       (H2 PostgreSQL mode for jOOQ codegen).
--       This Flyway-style script is provided for parity with the deployment DB (PostgreSQL).

CREATE TABLE IF NOT EXISTS certificates
(
    id                UUID         NOT NULL PRIMARY KEY,
    alias             VARCHAR(128) NOT NULL,
    subject           VARCHAR(1024) NOT NULL,
    issuer            VARCHAR(1024) NOT NULL,
    serial_number     VARCHAR(128) NOT NULL,
    not_before        TIMESTAMP    NOT NULL,
    not_after         TIMESTAMP    NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    pem_content       TEXT         NOT NULL,
    fingerprint       VARCHAR(128) NOT NULL,
    key_usage         VARCHAR(1024) NOT NULL DEFAULT '',
    subject_alt_names VARCHAR(2048) NOT NULL DEFAULT '',
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uq_certificates_fingerprint UNIQUE (fingerprint)
);

CREATE INDEX IF NOT EXISTS idx_certificates_status ON certificates (status);
CREATE INDEX IF NOT EXISTS idx_certificates_not_after ON certificates (not_after);
