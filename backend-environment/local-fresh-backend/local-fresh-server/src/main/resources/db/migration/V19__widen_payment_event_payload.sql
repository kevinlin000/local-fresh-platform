ALTER TABLE payment_event
  MODIFY COLUMN provider_reference VARCHAR(255) COMMENT '金流或付款請求參考值',
  MODIFY COLUMN raw_payload TEXT COMMENT '金流原始 payload 或本機請求內容';
