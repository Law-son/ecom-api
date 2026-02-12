-- Add inventory_status to existing inventory table (safe for existing data).
-- Run this on your database before deploying the app change.

-- Add column with a default so existing rows get a value (no NOT NULL violation).
ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS inventory_status VARCHAR(20) NOT NULL DEFAULT 'OUT_OF_STOCK';

-- Backfill: set IN_STOCK where quantity > 0.
UPDATE inventory
SET inventory_status = 'IN_STOCK'
WHERE quantity > 0;

-- Optional: constrain allowed values (uncomment if you want DB-level validation).
-- ALTER TABLE inventory
--     ADD CONSTRAINT chk_inventory_status
--     CHECK (inventory_status IN ('IN_STOCK', 'OUT_OF_STOCK', 'LOW_STOCK'));
