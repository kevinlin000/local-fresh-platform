-- Keep the admin dashboard from looking like an empty demo by refreshing a
-- small set of seeded orders into today's operations window.

UPDATE orders
SET status = CASE
        WHEN id = 9102 THEN 5
        WHEN id = 9103 THEN 3
        WHEN id IN (9104, 9105, 9106) THEN 2
        WHEN id = 9109 THEN 6
        ELSE status
    END,
    pay_status = CASE
        WHEN id IN (9102, 9103, 9104, 9105, 9106) THEN 1
        WHEN id = 9109 THEN 2
        ELSE pay_status
    END,
    order_time = CASE
        WHEN id = 9102 THEN DATE_SUB(NOW(), INTERVAL 4 HOUR)
        WHEN id = 9103 THEN DATE_SUB(NOW(), INTERVAL 3 HOUR)
        WHEN id = 9104 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        WHEN id = 9105 THEN DATE_SUB(NOW(), INTERVAL 110 MINUTE)
        WHEN id = 9106 THEN DATE_SUB(NOW(), INTERVAL 95 MINUTE)
        WHEN id = 9109 THEN DATE_SUB(NOW(), INTERVAL 5 HOUR)
        ELSE order_time
    END,
    checkout_time = CASE
        WHEN id = 9102 THEN DATE_SUB(NOW(), INTERVAL 4 HOUR)
        WHEN id = 9103 THEN DATE_SUB(NOW(), INTERVAL 3 HOUR)
        WHEN id = 9104 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        WHEN id = 9105 THEN DATE_SUB(NOW(), INTERVAL 110 MINUTE)
        WHEN id = 9106 THEN DATE_SUB(NOW(), INTERVAL 95 MINUTE)
        ELSE checkout_time
    END,
    estimated_delivery_time = CASE
        WHEN id = 9102 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        WHEN id = 9103 THEN DATE_ADD(NOW(), INTERVAL 2 HOUR)
        WHEN id IN (9104, 9105, 9106) THEN DATE_ADD(NOW(), INTERVAL 1 DAY)
        ELSE estimated_delivery_time
    END,
    delivery_time = CASE
        WHEN id = 9102 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        ELSE delivery_time
    END,
    cancel_reason = CASE
        WHEN id = 9109 THEN '會員取消'
        ELSE cancel_reason
    END,
    cancel_time = CASE
        WHEN id = 9109 THEN DATE_SUB(NOW(), INTERVAL 5 HOUR)
        ELSE cancel_time
    END
WHERE id IN (9102, 9103, 9104, 9105, 9106, 9109)
  AND number IN (
      'DEMO-20260615-002',
      'DEMO-20260617-003',
      'DEMO-GB-COMPLETE-A',
      'DEMO-GB-COMPLETE-B',
      'DEMO-GB-COMPLETE-C',
      'DEMO-20260614-004'
  );

UPDATE member
SET create_time = DATE_SUB(NOW(), INTERVAL 3 HOUR)
WHERE id = 9003
  AND openid = 'mock_user_c';
