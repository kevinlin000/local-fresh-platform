ALTER TABLE product_inventory_log
  ADD COLUMN idempotency_key VARCHAR(255) NULL AFTER operator_id,
  ADD UNIQUE KEY uk_inventory_log_idempotency_key (idempotency_key);
