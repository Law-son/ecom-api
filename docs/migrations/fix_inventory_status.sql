-- Fix inventory_status column for existing data
-- Run this script BEFORE starting the application to fix the NOT NULL constraint issue

-- Step 1: Add the column as nullable first (if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'inventory'
          AND column_name = 'inventory_status'
    ) THEN
        ALTER TABLE inventory
            ADD COLUMN inventory_status VARCHAR(50);
    END IF;
END $$;

-- Step 2: Backfill existing rows based on quantity
UPDATE inventory
SET inventory_status = CASE
    WHEN quantity = 0 THEN 'Out of stock'
    WHEN quantity = 1 THEN '1 unit in stock'
    WHEN quantity BETWEEN 2 AND 10 THEN quantity::text || ' units in stock'
    WHEN quantity BETWEEN 11 AND 15 THEN 'Few units in stock'
    ELSE 'In stock'
END
WHERE inventory_status IS NULL;

-- Step 3: Set default value for future inserts
ALTER TABLE inventory
    ALTER COLUMN inventory_status SET DEFAULT 'Out of stock';

-- Step 4: Make the column NOT NULL (now safe since all rows have values)
ALTER TABLE inventory
    ALTER COLUMN inventory_status SET NOT NULL;

