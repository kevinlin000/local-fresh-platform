ALTER TABLE product
  ADD COLUMN stock INT NOT NULL DEFAULT 100 COMMENT '可售庫存',
  ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 10 COMMENT '低庫存警示門檻',
  ADD INDEX idx_product_stock (stock);
