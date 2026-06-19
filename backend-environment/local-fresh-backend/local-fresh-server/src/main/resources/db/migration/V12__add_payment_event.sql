CREATE TABLE IF NOT EXISTS payment_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT COMMENT '訂單 id',
  order_number VARCHAR(50) NOT NULL COMMENT '訂單編號',
  provider VARCHAR(32) NOT NULL COMMENT '付款服務供應商',
  event_type VARCHAR(32) NOT NULL COMMENT '付款事件類型',
  provider_reference VARCHAR(128) COMMENT '金流或付款請求參考值',
  amount DECIMAL(10, 2) COMMENT '付款金額',
  result VARCHAR(32) NOT NULL COMMENT '事件處理結果',
  raw_payload VARCHAR(1000) COMMENT '金流原始 payload 或本機請求內容',
  created_at DATETIME NOT NULL COMMENT '事件建立時間',
  INDEX idx_payment_event_order_time (order_number, created_at),
  INDEX idx_payment_event_provider_ref (provider, provider_reference)
) COMMENT='付款事件紀錄';
