ALTER TABLE payment_event
  ADD COLUMN provider_trade_no VARCHAR(128) COMMENT '金流交易編號' AFTER provider_reference,
  ADD COLUMN idempotency_key VARCHAR(255) COMMENT '付款事件冪等鍵' AFTER provider_trade_no,
  ADD INDEX idx_payment_event_trade_no (provider, provider_trade_no),
  ADD INDEX idx_payment_event_idempotency_key (idempotency_key);

UPDATE payment_event
SET idempotency_key = CASE
    WHEN provider_reference IS NULL OR provider_reference = ''
      THEN CONCAT(provider, ':', event_type, ':', order_number)
    ELSE CONCAT(provider, ':', event_type, ':', order_number, ':', provider_reference)
  END
WHERE idempotency_key IS NULL;
