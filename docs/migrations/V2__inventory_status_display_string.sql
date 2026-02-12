-- Add or convert inventory_status to display string (run top to bottom).
-- Works whether the column already exists (from V1) or not.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'inventory'
          AND column_name = 'inventory_status'
    ) THEN
        ALTER TABLE inventory
            ADD COLUMN inventory_status VARCHAR(50) NOT NULL DEFAULT 'Out of stock';
    ELSE
        ALTER TABLE inventory
            ALTER COLUMN inventory_status TYPE VARCHAR(50);
        ALTER TABLE inventory
            ALTER COLUMN inventory_status SET DEFAULT 'Out of stock';
    END IF;
END $$;

-- Backfill from quantity: 0 -> Out of stock, 1 -> 1 unit in stock, 2-10 -> N units in stock, 11-15 -> Few units in stock, 16+ -> In stock.
UPDATE inventory
SET inventory_status = CASE
    WHEN quantity = 0 THEN 'Out of stock'
    WHEN quantity = 1 THEN '1 unit in stock'
    WHEN quantity BETWEEN 2 AND 10 THEN quantity::text || ' units in stock'
    WHEN quantity BETWEEN 11 AND 15 THEN 'Few units in stock'
    ELSE 'In stock'
END;
