# Database migrations

Run these **once** on your existing database when upgrading. Order matters if multiple scripts exist.

- **V1__add_inventory_status.sql** – Adds `inventory_status` to `inventory`. Safe for existing data: new column has a default and is backfilled from `quantity`.
- **V2__inventory_status_display_string.sql** – Converts `inventory_status` to display strings: "Out of stock", "N unit(s) in stock", "Few units in stock", "In stock". Run after V1 (or if you already have `inventory_status`).
- **V3__add_optimistic_locking_columns.sql** – Adds JPA `@Version` columns (`version`) to `inventory` and `orders` for optimistic locking.
