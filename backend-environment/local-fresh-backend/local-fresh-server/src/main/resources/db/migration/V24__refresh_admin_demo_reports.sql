-- Keep admin reports and audit pages useful for portfolio demos.
-- These fixed DEMO-RPT rows are synthetic evidence data; they are refreshed
-- only inside the demo id range and do not touch real customer orders.

UPDATE member
SET create_time = CASE
    WHEN id = 9001 THEN DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 9 HOUR)
    WHEN id = 9002 THEN DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY), INTERVAL 14 HOUR)
    WHEN id = 9003 THEN DATE_ADD(CURDATE(), INTERVAL 11 HOUR)
    ELSE create_time
END
WHERE id IN (9001, 9002, 9003)
  AND openid IN ('mock_user_a', 'mock_user_b', 'mock_user_c');

DELETE FROM admin_operation_log
WHERE target_id IN (9401, 9402, 9403, 9404, 9405, 9406, 9407, 9408, 9409, 9410, 9411)
   OR reason LIKE 'Demo report seed:%';

DELETE FROM order_detail
WHERE order_id IN (9401, 9402, 9403, 9404, 9405, 9406, 9407, 9408, 9409, 9410, 9411);

DELETE FROM orders
WHERE id IN (9401, 9402, 9403, 9404, 9405, 9406, 9407, 9408, 9409, 9410, 9411)
   OR number LIKE 'DEMO-RPT-%';

INSERT INTO orders
    (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status,
     amount, remark, user_name, phone, address, consignee, cancel_reason, rejection_reason, cancel_time,
     estimated_delivery_time, delivery_status, delivery_time, pack_amount, tableware_number, tableware_status)
VALUES
    (9401, 'DEMO-RPT-D6-001', 5, 9001, 9001,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 10 HOUR),
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 10 HOUR), 1, 1,
     430.00, 'Demo report seed: 家庭蔬菜補貨', '林品安', '0912001001',
     '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 14 HOUR), 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 13 HOUR), 10, 0, 0),
    (9402, 'DEMO-RPT-D5-001', 5, 9002, 9003,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 5 DAY), INTERVAL 11 HOUR),
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 5 DAY), INTERVAL 11 HOUR), 1, 1,
     680.00, 'Demo report seed: 海鮮直送', '陳柏翰', '0912001002',
     '台灣台北市松山區民生東路五段 88 號', '陳柏翰', NULL, NULL, NULL,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 5 DAY), INTERVAL 15 HOUR), 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 5 DAY), INTERVAL 14 HOUR), 10, 0, 0),
    (9403, 'DEMO-RPT-D4-001', 5, 9001, 9002,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 4 DAY), INTERVAL 12 HOUR),
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 4 DAY), INTERVAL 12 HOUR), 1, 1,
     520.00, 'Demo report seed: 辦公室共煮', '林品安', '0912001001',
     '台灣新北市新店區北新路三段 120 號', '林品安', NULL, NULL, NULL,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 4 DAY), INTERVAL 16 HOUR), 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 4 DAY), INTERVAL 15 HOUR), 10, 0, 0),
    (9404, 'DEMO-RPT-D3-001', 5, 9003, 9004,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY), INTERVAL 9 HOUR),
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY), INTERVAL 9 HOUR), 1, 1,
     980.00, 'Demo report seed: 家庭週末箱', '黃郁庭', '0912001003',
     '台灣桃園市中壢區中大路 300 號', '黃郁庭', NULL, NULL, NULL,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY), INTERVAL 14 HOUR), 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY), INTERVAL 13 HOUR), 10, 0, 0),
    (9405, 'DEMO-RPT-D2-001', 5, 9002, 9003,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 13 HOUR),
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 13 HOUR), 1, 1,
     760.00, 'Demo report seed: 肉品與鮮奶', '陳柏翰', '0912001002',
     '台灣台北市松山區民生東路五段 88 號', '陳柏翰', NULL, NULL, NULL,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 17 HOUR), 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 16 HOUR), 10, 0, 0),
    (9406, 'DEMO-RPT-D1-001', 5, 9001, 9001,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 10 HOUR),
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 10 HOUR), 1, 1,
     1120.00, 'Demo report seed: 大家庭採買', '林品安', '0912001001',
     '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 15 HOUR), 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR), 10, 0, 0),
    (9407, 'DEMO-RPT-D0-001', 5, 9003, 9004,
     DATE_ADD(CURDATE(), INTERVAL 9 HOUR),
     DATE_ADD(CURDATE(), INTERVAL 9 HOUR), 1, 1,
     890.00, 'Demo report seed: 今日完成訂單', '黃郁庭', '0912001003',
     '台灣桃園市中壢區中大路 300 號', '黃郁庭', NULL, NULL, NULL,
     DATE_ADD(CURDATE(), INTERVAL 14 HOUR), 1,
     DATE_ADD(CURDATE(), INTERVAL 13 HOUR), 10, 0, 0),
    (9408, 'DEMO-RPT-D0-002', 2, 9001, 9001,
     DATE_ADD(CURDATE(), INTERVAL 10 HOUR),
     DATE_ADD(CURDATE(), INTERVAL 10 HOUR), 1, 1,
     590.00, 'Demo report seed: 今日待確認', '林品安', '0912001001',
     '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_ADD(CURDATE(), INTERVAL 17 HOUR), 1, NULL, 10, 0, 0),
    (9409, 'DEMO-RPT-D0-003', 3, 9002, 9003,
     DATE_ADD(CURDATE(), INTERVAL 10 HOUR) + INTERVAL 30 MINUTE,
     DATE_ADD(CURDATE(), INTERVAL 10 HOUR) + INTERVAL 30 MINUTE, 1, 1,
     320.00, 'Demo report seed: 今日已確認', '陳柏翰', '0912001002',
     '台灣台北市松山區民生東路五段 88 號', '陳柏翰', NULL, NULL, NULL,
     DATE_ADD(CURDATE(), INTERVAL 18 HOUR), 1, NULL, 10, 0, 0),
    (9410, 'DEMO-RPT-D0-004', 4, 9001, 9002,
     DATE_ADD(CURDATE(), INTERVAL 11 HOUR),
     DATE_ADD(CURDATE(), INTERVAL 11 HOUR), 1, 1,
     375.00, 'Demo report seed: 今日配送中', '林品安', '0912001001',
     '台灣新北市新店區北新路三段 120 號', '林品安', NULL, NULL, NULL,
     DATE_ADD(CURDATE(), INTERVAL 16 HOUR), 1, NULL, 10, 0, 0),
    (9411, 'DEMO-RPT-D0-005', 6, 9003, 9004,
     DATE_ADD(CURDATE(), INTERVAL 8 HOUR),
     DATE_ADD(CURDATE(), INTERVAL 8 HOUR), 1, 2,
     280.00, 'Demo report seed: 今日取消', '黃郁庭', '0912001003',
     '台灣桃園市中壢區中大路 300 號', '黃郁庭', '會員臨時改期', NULL,
     DATE_ADD(CURDATE(), INTERVAL 8 HOUR) + INTERVAL 40 MINUTE,
     NULL, 1, NULL, 0, 0, 0);

INSERT INTO order_detail
    (name, image, order_id, product_id, gift_box_id, product_spec, number, amount)
VALUES
    ('有機高麗菜 (1顆)', '/demo-assets/product-2.png', 9401, 2, NULL, NULL, 2, 78.00),
    ('宜蘭三星蔥 (300g)', '/demo-assets/product-7.png', 9401, 7, NULL, NULL, 1, 95.00),
    ('文山牧場鮮奶 (936ml)', '/demo-assets/product-19.png', 9401, 19, NULL, NULL, 1, 179.00),
    ('海鮮三日鮮', '/demo-assets/gift-box-2.png', 9402, NULL, 2, NULL, 1, 680.00),
    ('媽媽家常組合', '/demo-assets/gift-box-5.png', 9403, NULL, 5, NULL, 1, 520.00),
    ('週末蔬菜箱', '/demo-assets/gift-box-1.png', 9404, NULL, 1, NULL, 1, 480.00),
    ('嘉義雞胸肉 (500g)', '/demo-assets/product-10.png', 9404, 10, NULL, NULL, 2, 240.00),
    ('嘉義雞胸肉 (500g)', '/demo-assets/product-10.png', 9405, 10, NULL, NULL, 2, 240.00),
    ('文山牧場鮮奶 (936ml)', '/demo-assets/product-19.png', 9405, 19, NULL, NULL, 1, 280.00),
    ('週末蔬菜箱', '/demo-assets/gift-box-1.png', 9406, NULL, 1, NULL, 2, 480.00),
    ('池上越光米 (2kg)', '/demo-assets/product-22.png', 9406, 22, NULL, NULL, 1, 160.00),
    ('露營野炊組', '/demo-assets/gift-box-6.png', 9407, NULL, 6, NULL, 1, 750.00),
    ('有機高麗菜 (1顆)', '/demo-assets/product-2.png', 9407, 2, NULL, NULL, 1, 140.00),
    ('雙人輕煮組', '/demo-assets/gift-box-3.png', 9408, NULL, 3, NULL, 1, 590.00),
    ('早餐專屬箱', '/demo-assets/gift-box-4.png', 9409, NULL, 4, NULL, 1, 320.00),
    ('澎湖花枝 (1隻 約400g)', '/demo-assets/product-15.png', 9410, 15, NULL, NULL, 1, 280.00),
    ('宜蘭三星蔥 (300g)', '/demo-assets/product-7.png', 9410, 7, NULL, NULL, 1, 95.00),
    ('池上越光米 (2kg)', '/demo-assets/product-22.png', 9411, 22, NULL, NULL, 1, 280.00);

INSERT INTO admin_operation_log
    (action, target_type, target_id, before_value, after_value, reason, operator_type, operator_id, created_at)
VALUES
    ('ORDER_CONFIRM', 'ORDER', 9408, '2', '3', 'Demo report seed: 門市確認今日待出貨訂單', 'ADMIN', 1,
     DATE_ADD(CURDATE(), INTERVAL 10 HOUR) + INTERVAL 20 MINUTE),
    ('ORDER_START_DELIVERY', 'ORDER', 9410, '3', '4', 'Demo report seed: 配送員已取貨', 'ADMIN', 1,
     DATE_ADD(CURDATE(), INTERVAL 11 HOUR) + INTERVAL 15 MINUTE),
    ('ORDER_COMPLETE', 'ORDER', 9407, '4', '5', 'Demo report seed: 客戶已簽收', 'ADMIN', 1,
     DATE_ADD(CURDATE(), INTERVAL 13 HOUR)),
    ('ORDER_CANCEL', 'ORDER', 9411, '2', '6', 'Demo report seed: 會員臨時改期', 'ADMIN', 1,
     DATE_ADD(CURDATE(), INTERVAL 8 HOUR) + INTERVAL 40 MINUTE),
    ('ORDER_COMPLETE', 'ORDER', 9406, '4', '5', 'Demo report seed: 前日配送完成', 'ADMIN', 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR)),
    ('PRODUCT_INVENTORY_ADJUST', 'PRODUCT', 2, '18', '42', 'Demo report seed: 早市補進高麗菜', 'ADMIN', 1,
     DATE_ADD(CURDATE(), INTERVAL 7 HOUR) + INTERVAL 30 MINUTE),
    ('PRODUCT_INVENTORY_ADJUST', 'PRODUCT', 10, '9', '24', 'Demo report seed: 雞胸肉到貨入庫', 'ADMIN', 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 8 HOUR)),
    ('PRODUCT_INVENTORY_ADJUST', 'PRODUCT', 19, '15', '11', 'Demo report seed: 鮮奶出貨扣庫', 'ADMIN', 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 16 HOUR)),
    ('ORDER_CONFIRM', 'ORDER', 9405, '2', '3', 'Demo report seed: 前兩日訂單確認', 'ADMIN', 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 13 HOUR) + INTERVAL 20 MINUTE),
    ('ORDER_START_DELIVERY', 'ORDER', 9405, '3', '4', 'Demo report seed: 前兩日訂單配送', 'ADMIN', 1,
     DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 15 HOUR));
