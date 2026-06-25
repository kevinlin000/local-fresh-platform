UPDATE orders
SET number = CONCAT('LF-', SUBSTRING(number, 6))
WHERE number LIKE 'DEMO-%';

UPDATE payment_event
SET
  order_number = CONCAT('LF-', SUBSTRING(order_number, 6)),
  provider_reference = REPLACE(provider_reference, 'demo-paid:DEMO-', 'demo-paid:LF-'),
  idempotency_key = REPLACE(idempotency_key, ':DEMO-', ':LF-'),
  raw_payload = REPLACE(raw_payload, 'demo-paid:DEMO-', 'demo-paid:LF-')
WHERE order_number LIKE 'DEMO-%';

UPDATE group_buy
SET group_no = CASE group_no
  WHEN 'GB-DEMO-ACTIVE' THEN 'GB-202606-ACTIVE'
  WHEN 'GB-DEMO-COMPLETE' THEN 'GB-202606-COMPLETE'
  ELSE group_no
END
WHERE group_no LIKE 'GB-DEMO-%';
