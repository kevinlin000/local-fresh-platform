-- V10__demo_journey_seed.sql
-- 補齊作品展示用會員旅程資料:試用會員、配送地址、購物車、訂單與揪團。
-- 這批資料使用固定 DEMO 前綴與 9000+ id，方便辨識與重置。

-- ============================================================
-- 第一步:清掉同一批 demo journey 資料
-- ============================================================

DELETE FROM group_buy_participant
WHERE group_buy_id IN (
    SELECT id FROM group_buy WHERE group_no LIKE 'GB-DEMO-%'
)
   OR member_id IN (9001, 9002, 9003)
   OR pre_order_id IN (9104, 9105, 9106, 9107, 9108);

DELETE FROM group_buy
WHERE group_no LIKE 'GB-DEMO-%'
   OR id IN (9201, 9202);

DELETE FROM order_detail
WHERE order_id IN (
    SELECT id FROM orders WHERE number LIKE 'DEMO-%'
)
   OR order_id IN (9101, 9102, 9103, 9104, 9105, 9106, 9107, 9108, 9109);

DELETE FROM cart
WHERE id IN (9001, 9002, 9003)
   OR user_id IN (9001, 9002, 9003);

DELETE FROM orders
WHERE number LIKE 'DEMO-%'
   OR id IN (9101, 9102, 9103, 9104, 9105, 9106, 9107, 9108, 9109);

DELETE FROM shipping_address
WHERE id IN (9001, 9002, 9003, 9004)
   OR member_id IN (9001, 9002, 9003);

DELETE FROM member
WHERE id IN (9001, 9002, 9003)
   OR openid IN ('mock_user_a', 'mock_user_b', 'mock_user_c')
   OR google_sub IN ('demo-user-a', 'demo-user-b', 'demo-user-c');

-- ============================================================
-- 第二步:試用會員
-- 對應前端快速登入 code: user_a / user_b / user_c
-- ============================================================

INSERT INTO member
    (id, openid, google_sub, email, name, phone, sex, avatar, avatar_url, login_provider, create_time)
VALUES
    (9001, 'mock_user_a', 'demo-user-a', 'demo.member.a@example.com', '林品安', '0912001001', '2', NULL, NULL, 'mock', DATE_SUB(NOW(), INTERVAL 21 DAY)),
    (9002, 'mock_user_b', 'demo-user-b', 'demo.member.b@example.com', '陳柏翰', '0912001002', '1', NULL, NULL, 'mock', DATE_SUB(NOW(), INTERVAL 18 DAY)),
    (9003, 'mock_user_c', 'demo-user-c', 'demo.member.c@example.com', '黃郁庭', '0912001003', '2', NULL, NULL, 'mock', DATE_SUB(NOW(), INTERVAL 12 DAY));

-- ============================================================
-- 第三步:配送地址
-- ============================================================

INSERT INTO shipping_address
    (id, member_id, consignee, phone, sex, province_code, province_name, city_code, city_name,
     district_code, district_name, detail, label, is_default)
VALUES
    (9001, 9001, '林品安', '0912001001', '2', 'TW', '台灣', 'TPE', '台北市', 'DAAN', '大安區', '羅斯福路四段 1 號 8 樓', '住家', 1),
    (9002, 9001, '林品安', '0912001001', '2', 'TW', '台灣', 'NTP', '新北市', 'XINDIAN', '新店區', '北新路三段 120 號', '公司', 0),
    (9003, 9002, '陳柏翰', '0912001002', '1', 'TW', '台灣', 'TPE', '台北市', 'SONGSHAN', '松山區', '民生東路五段 88 號', '住家', 1),
    (9004, 9003, '黃郁庭', '0912001003', '2', 'TW', '台灣', 'TAO', '桃園市', 'ZHONGLI', '中壢區', '中大路 300 號', '住家', 1);

-- ============================================================
-- 第四步:購物車
-- amount 為單價，number 為數量。
-- ============================================================

INSERT INTO cart
    (id, name, user_id, product_id, gift_box_id, product_spec, number, amount, image, create_time)
VALUES
    (9001, '有機高麗菜 (1顆)', 9001, 2, NULL, NULL, 1, 78.00, '/demo-assets/product-leaf.svg', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
    (9002, '雙人輕煮組', 9001, NULL, 3, NULL, 1, 590.00, '/demo-assets/gift-box.svg', DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
    (9003, '宜蘭三星蔥 (300g)', 9002, 7, NULL, NULL, 2, 95.00, '/demo-assets/product-root.svg', DATE_SUB(NOW(), INTERVAL 16 MINUTE));

-- ============================================================
-- 第五步:一般訂單與揪團預訂單
-- ============================================================

INSERT INTO orders
    (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status,
     amount, remark, user_name, phone, address, consignee, cancel_reason, rejection_reason, cancel_time,
     estimated_delivery_time, delivery_status, delivery_time, pack_amount, tableware_number, tableware_status)
VALUES
    (9101, 'DEMO-20260617-001', 1, 9001, 9001, DATE_SUB(NOW(), INTERVAL 5 MINUTE), NULL, 1, 0,
     220.00, '今晚煮青菜蛋花湯', '林品安', '0912001001', '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 4 HOUR), 1, NULL, 0, 0, 0),
    (9102, 'DEMO-20260615-002', 5, 9001, 9001, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 1, 1,
     760.00, '週末家庭晚餐', '林品安', '0912001001', '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_SUB(NOW(), INTERVAL 1 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, 0, 0),
    (9103, 'DEMO-20260617-003', 4, 9001, 9002, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), 1, 1,
     375.00, '公司晚餐共煮', '林品安', '0912001001', '台灣新北市新店區北新路三段 120 號', '林品安', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 90 MINUTE), 1, NULL, 0, 0, 0),
    (9104, 'DEMO-GB-COMPLETE-A', 2, 9001, 9001, DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR), 1, 1,
     95.00, '已成團，等店家確認', '林品安', '0912001001', '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 1 DAY), 1, NULL, 0, 0, 0),
    (9105, 'DEMO-GB-COMPLETE-B', 2, 9002, 9003, DATE_SUB(NOW(), INTERVAL 19 HOUR), DATE_SUB(NOW(), INTERVAL 19 HOUR), 1, 1,
     95.00, '跟團購買三星蔥', '陳柏翰', '0912001002', '台灣台北市松山區民生東路五段 88 號', '陳柏翰', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 1 DAY), 1, NULL, 0, 0, 0),
    (9106, 'DEMO-GB-COMPLETE-C', 2, 9003, 9004, DATE_SUB(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 18 HOUR), 1, 1,
     95.00, '跟團購買三星蔥', '黃郁庭', '0912001003', '台灣桃園市中壢區中大路 300 號', '黃郁庭', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 1 DAY), 1, NULL, 0, 0, 0),
    (9107, 'DEMO-GB-ACTIVE-A', 8, 9001, 9001, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 1, 0,
     220.00, '揪團中，差一位成團', '林品安', '0912001001', '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 1 DAY), 1, NULL, 0, 0, 0),
    (9108, 'DEMO-GB-ACTIVE-B', 8, 9002, 9003, DATE_SUB(NOW(), INTERVAL 90 MINUTE), DATE_SUB(NOW(), INTERVAL 90 MINUTE), 1, 0,
     220.00, '揪團中，差一位成團', '陳柏翰', '0912001002', '台灣台北市松山區民生東路五段 88 號', '陳柏翰', NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL 1 DAY), 1, NULL, 0, 0, 0),
    (9109, 'DEMO-20260614-004', 6, 9001, 9001, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 1, 2,
     110.00, '臨時改期取消', '林品安', '0912001001', '台灣台北市大安區羅斯福路四段 1 號 8 樓', '林品安', '會員取消', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY),
     NULL, 1, NULL, 0, 0, 0);

INSERT INTO order_detail
    (name, image, order_id, product_id, gift_box_id, product_spec, number, amount)
VALUES
    ('有機 A 菜 (300g)', '/demo-assets/product-leaf.svg', 9101, 1, NULL, NULL, 2, 45.00),
    ('本土無毒土雞蛋 (10入)', '/demo-assets/product-dairy.svg', 9101, 20, NULL, NULL, 1, 130.00),
    ('週末蔬菜箱', '/demo-assets/gift-box.svg', 9102, NULL, 1, NULL, 1, 480.00),
    ('池上越光米 (2kg)', '/demo-assets/product-grain.svg', 9102, 22, NULL, NULL, 1, 280.00),
    ('澎湖花枝 (1隻 約400g)', '/demo-assets/product-seafood.svg', 9103, 15, NULL, NULL, 1, 280.00),
    ('宜蘭三星蔥 (300g)', '/demo-assets/product-root.svg', 9103, 7, NULL, NULL, 1, 95.00),
    ('宜蘭三星蔥 (300g)', '/demo-assets/product-root.svg', 9104, 7, NULL, NULL, 1, 95.00),
    ('宜蘭三星蔥 (300g)', '/demo-assets/product-root.svg', 9105, 7, NULL, NULL, 1, 95.00),
    ('宜蘭三星蔥 (300g)', '/demo-assets/product-root.svg', 9106, 7, NULL, NULL, 1, 95.00),
    ('雲林溫體豬五花 (500g)', '/demo-assets/product-protein.svg', 9107, 11, NULL, NULL, 1, 220.00),
    ('雲林溫體豬五花 (500g)', '/demo-assets/product-protein.svg', 9108, 11, NULL, NULL, 1, 220.00),
    ('文山牧場鮮奶 (936ml)', '/demo-assets/product-dairy.svg', 9109, 19, NULL, NULL, 1, 110.00);

-- ============================================================
-- 第六步:揪團資料
-- status:1 揪團中, 2 已成團, 3 已失敗, 4 已取消
-- ============================================================

INSERT INTO group_buy
    (id, group_no, initiator_id, status, required_count, current_count, expire_at, created_at, updated_at)
VALUES
    (9201, 'GB-DEMO-ACTIVE', 9001, 1, 3, 2, DATE_ADD(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (9202, 'GB-DEMO-COMPLETE', 9001, 2, 3, 3, DATE_ADD(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 18 HOUR));

INSERT INTO group_buy_participant
    (id, group_buy_id, member_id, pre_order_id, joined_at)
VALUES
    (9301, 9201, 9001, 9107, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (9302, 9201, 9002, 9108, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (9303, 9202, 9001, 9104, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    (9304, 9202, 9002, 9105, DATE_SUB(NOW(), INTERVAL 19 HOUR)),
    (9305, 9202, 9003, 9106, DATE_SUB(NOW(), INTERVAL 18 HOUR));
