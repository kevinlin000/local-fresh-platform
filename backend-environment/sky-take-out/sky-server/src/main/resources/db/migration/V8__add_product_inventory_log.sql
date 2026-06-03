CREATE TABLE IF NOT EXISTS product_inventory_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  change_quantity INT NOT NULL,
  stock_before INT NOT NULL,
  stock_after INT NOT NULL,
  reason VARCHAR(64) NOT NULL,
  reference_type VARCHAR(32),
  reference_id BIGINT,
  operator_type VARCHAR(32) NOT NULL,
  operator_id BIGINT,
  created_at DATETIME NOT NULL,
  INDEX idx_inventory_log_product_time (product_id, created_at),
  INDEX idx_inventory_log_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
