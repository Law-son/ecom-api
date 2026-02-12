-- Add optimistic locking columns for high-contention JPA entities.
-- Safe for existing data: defaults initialize existing rows.

ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
