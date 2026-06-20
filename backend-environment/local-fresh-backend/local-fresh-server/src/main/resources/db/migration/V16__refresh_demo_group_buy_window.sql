-- Keep the reviewer-facing demo group-buy journey from expiring shortly after
-- a local or cloud demo database is created.

UPDATE group_buy
SET status = 1,
    required_count = 3,
    current_count = 2,
    expire_at = DATE_ADD(NOW(), INTERVAL 30 DAY),
    created_at = DATE_SUB(NOW(), INTERVAL 2 HOUR),
    updated_at = DATE_SUB(NOW(), INTERVAL 90 MINUTE)
WHERE group_no = 'GB-DEMO-ACTIVE';

UPDATE orders
SET status = 8,
    pay_status = 0,
    cancel_reason = NULL,
    rejection_reason = NULL,
    cancel_time = NULL,
    order_time = CASE
        WHEN id = 9107 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        WHEN id = 9108 THEN DATE_SUB(NOW(), INTERVAL 90 MINUTE)
        ELSE order_time
    END,
    checkout_time = CASE
        WHEN id = 9107 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
        WHEN id = 9108 THEN DATE_SUB(NOW(), INTERVAL 90 MINUTE)
        ELSE checkout_time
    END,
    estimated_delivery_time = DATE_ADD(NOW(), INTERVAL 1 DAY)
WHERE id IN (9107, 9108)
  AND number IN ('DEMO-GB-ACTIVE-A', 'DEMO-GB-ACTIVE-B');

UPDATE group_buy_participant
SET joined_at = CASE
    WHEN id = 9301 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR)
    WHEN id = 9302 THEN DATE_SUB(NOW(), INTERVAL 90 MINUTE)
    ELSE joined_at
END
WHERE id IN (9301, 9302)
  AND group_buy_id = 9201;
