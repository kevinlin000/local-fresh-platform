-- V5__taiwan_localization.sql
-- 台灣化:重設分類、單品、直送箱為台灣在地生鮮平台假資料
-- 執行前提:V1 已建立目前在地生鮮平台資料表

-- ============================================================
-- 第一步:清空舊資料(蒼穹原餐廳資料 + 訂單關聯)
-- 順序:依外鍵順序由下往上清
-- ============================================================

-- 清訂單明細(orders 不清,保留 demo 帳號歷史)
DELETE FROM order_detail;
DELETE FROM orders;

-- 清購物車
DELETE FROM cart;

-- 清揪團相關(避免揪團資料對應到不存在的商品)
DELETE FROM group_buy_participant;
DELETE FROM group_buy;

-- 清直送箱與單品關聯
DELETE FROM gift_box_product;
DELETE FROM gift_box;

-- 清單品規格與單品
DELETE FROM product_spec;
DELETE FROM product;

-- 清分類
DELETE FROM category;

-- 重置 AUTO_INCREMENT
ALTER TABLE category AUTO_INCREMENT = 1;
ALTER TABLE product AUTO_INCREMENT = 1;
ALTER TABLE product_spec AUTO_INCREMENT = 1;
ALTER TABLE gift_box AUTO_INCREMENT = 1;
ALTER TABLE gift_box_product AUTO_INCREMENT = 1;

-- ============================================================
-- 第二步:新增 8 個分類
-- type:1 = 單品分類, 2 = 直送箱分類
-- ============================================================

INSERT INTO category (id, type, name, sort, status, create_time, update_time, create_user, update_user) VALUES
(1, 1, '葉菜類', 1, 1, NOW(), NOW(), 1, 1),
(2, 1, '根莖類', 2, 1, NOW(), NOW(), 1, 1),
(3, 1, '肉品類', 3, 1, NOW(), NOW(), 1, 1),
(4, 1, '海鮮類', 4, 1, NOW(), NOW(), 1, 1),
(5, 1, '蛋奶類', 5, 1, NOW(), NOW(), 1, 1),
(6, 1, '雜糧類', 6, 1, NOW(), NOW(), 1, 1),
(7, 1, '調味料', 7, 1, NOW(), NOW(), 1, 1),
(8, 2, '嚴選直送箱', 1, 1, NOW(), NOW(), 1, 1);

-- ============================================================
-- 第三步:新增約 30 個單品(食材)
-- ============================================================

-- 葉菜類 (category_id = 1)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(1, '有機 A 菜 (300g)', 45.00, '', '宜蘭友善農場直送,清炒或燙拌皆宜。', 1, NOW(), NOW(), 1, 1),
(1, '有機高麗菜 (1顆)', 78.00, '', '雲林斗六農會契作,纖維細甜度足。', 1, NOW(), NOW(), 1, 1),
(1, '青江菜 (300g)', 38.00, '', '彰化平地當季蔬菜,水煮快炒兩相宜。', 1, NOW(), NOW(), 1, 1),
(1, '空心菜 (400g)', 35.00, '', '本土水耕,口感脆嫩無泥味。', 1, NOW(), NOW(), 1, 1),
(1, '地瓜葉 (400g)', 30.00, '', '南投當季,川燙佐蒜末或醬油膏皆好。', 1, NOW(), NOW(), 1, 1);

-- 根莖類 (category_id = 2)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(2, '池上蘿蔔 (1kg)', 65.00, '', '台東池上產,清燉燒煮甜度高。', 1, NOW(), NOW(), 1, 1),
(2, '宜蘭三星蔥 (300g)', 95.00, '', '三星鄉指定產地,蔥白脆甜。', 1, NOW(), NOW(), 1, 1),
(2, '屏東紅洋蔥 (1kg)', 60.00, '', '恆春半島強風日照,辣度低甜度高。', 1, NOW(), NOW(), 1, 1),
(2, '大甲牛蒡 (500g)', 88.00, '', '台中大甲特產,涼拌或燉湯皆宜。', 1, NOW(), NOW(), 1, 1),
(2, '台農 57 號地瓜 (1kg)', 72.00, '', '雲林當季鮮挖,蒸烤香甜鬆軟。', 1, NOW(), NOW(), 1, 1);

-- 肉品類 (category_id = 3)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(3, '雲林溫體豬五花 (500g)', 220.00, '', '當日清晨屠宰,適合滷或紅燒。', 1, NOW(), NOW(), 1, 1),
(3, '嘉義雞胸肉 (500g)', 150.00, '', '嘉義養雞場,低脂高蛋白。', 1, NOW(), NOW(), 1, 1),
(3, '台南古早雞土雞腿 (2隻)', 280.00, '', '台南放山土雞,適合三杯或燒烤。', 1, NOW(), NOW(), 1, 1),
(3, '本土豬絞肉 (300g)', 130.00, '', '當日現絞,適合包水餃或炒肉燥。', 1, NOW(), NOW(), 1, 1);

-- 海鮮類 (category_id = 4)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(4, '澎湖花枝 (1隻 約400g)', 280.00, '', '澎湖海域捕撈,當日急凍直送。', 1, NOW(), NOW(), 1, 1),
(4, '東港鯛魚清肉 (300g)', 220.00, '', '屏東東港養殖,無刺去皮。', 1, NOW(), NOW(), 1, 1),
(4, '宜蘭吻仔魚 (200g)', 180.00, '', '宜蘭近海,每日新鮮捕撈。', 1, NOW(), NOW(), 1, 1),
(4, '屏東黑鯛 (1尾 約500g)', 320.00, '', '屏東魚塭養殖,清蒸最佳。', 1, NOW(), NOW(), 1, 1);

-- 蛋奶類 (category_id = 5)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(5, '文山牧場鮮奶 (936ml)', 110.00, '', '南投文山牧場,72 度低溫殺菌。', 1, NOW(), NOW(), 1, 1),
(5, '本土無毒土雞蛋 (10入)', 130.00, '', '雲林平飼土雞蛋,蛋黃濃郁。', 1, NOW(), NOW(), 1, 1),
(5, '田媽媽鵪鶉蛋 (20入)', 65.00, '', '彰化在地鵪鶉場,殼薄黃多。', 1, NOW(), NOW(), 1, 1);

-- 雜糧類 (category_id = 6)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(6, '池上越光米 (2kg)', 280.00, '', '台東池上一期米,粒粒晶瑩。', 1, NOW(), NOW(), 1, 1),
(6, '花蓮有機糙米 (2kg)', 320.00, '', '花蓮富里,保留胚芽營養。', 1, NOW(), NOW(), 1, 1),
(6, '台南青仁黑豆 (500g)', 150.00, '', '台南本土種植,炒香煮湯皆宜。', 1, NOW(), NOW(), 1, 1),
(6, '雲林毛綠豆 (500g)', 95.00, '', '雲林虎尾,煮綠豆湯首選。', 1, NOW(), NOW(), 1, 1);

-- 調味料 (category_id = 7)
INSERT INTO product (category_id, product_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(7, '屏東日曬海鹽 (300g)', 120.00, '', '屏東佳冬鹽田,礦物質豐富。', 1, NOW(), NOW(), 1, 1),
(7, '雲林豆瓣醬 (400g)', 180.00, '', '西螺老字號,純釀無添加。', 1, NOW(), NOW(), 1, 1),
(7, '宜蘭三星蔥油 (200ml)', 220.00, '', '三星蔥冷壓,拌麵拌飯香氣足。', 1, NOW(), NOW(), 1, 1),
(7, '台東產麻油 (250ml)', 280.00, '', '台東關山小磨麻油,香醇純正。', 1, NOW(), NOW(), 1, 1);

-- ============================================================
-- 第四步:新增 6 個直送箱
-- ============================================================

INSERT INTO gift_box (id, category_id, box_name, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
(1, 8, '週末蔬菜箱', 480.00, '', '葉菜類 3 種 + 根莖類 3 種,夠 4 人份家庭一週使用。', 1, NOW(), NOW(), 1, 1),
(2, 8, '海鮮三日鮮', 680.00, '', '鯛魚 + 花枝 + 吻仔魚,當日海港直送。', 1, NOW(), NOW(), 1, 1),
(3, 8, '雙人輕煮組', 590.00, '', '1 種肉品 + 2 種蔬菜 + 池上米,雙人三日無煩惱。', 1, NOW(), NOW(), 1, 1),
(4, 8, '早餐專屬箱', 320.00, '', '鮮奶 + 雞蛋 + 鵪鶉蛋,週一到週五早餐齊全。', 1, NOW(), NOW(), 1, 1),
(5, 8, '媽媽家常組合', 520.00, '', '經典家常 5 種食材,輕鬆煮出三菜一湯。', 1, NOW(), NOW(), 1, 1),
(6, 8, '露營野炊組', 750.00, '', '即食肉品 + 蔬菜 + 雜糧,露營野炊一箱搞定。', 1, NOW(), NOW(), 1, 1);

-- ============================================================
-- 第五步:直送箱 ↔ 單品關聯
-- 假設前面的 product id 從 1 開始累計到 30 (8 大類食材)
-- ============================================================

-- 週末蔬菜箱 (gift_box_id = 1):3 葉菜 + 3 根莖
INSERT INTO gift_box_product (gift_box_id, product_id, copies) VALUES
(1, 1, 1),  -- 有機 A 菜
(1, 2, 1),  -- 有機高麗菜
(1, 5, 1),  -- 地瓜葉
(1, 6, 1),  -- 池上蘿蔔
(1, 7, 1),  -- 宜蘭三星蔥
(1, 10, 1); -- 台農 57 號地瓜

-- 海鮮三日鮮 (gift_box_id = 2):3 種海鮮
INSERT INTO gift_box_product (gift_box_id, product_id, copies) VALUES
(2, 15, 1), -- 澎湖花枝
(2, 16, 1), -- 東港鯛魚清肉
(2, 17, 1); -- 宜蘭吻仔魚

-- 雙人輕煮組 (gift_box_id = 3):1 肉 + 2 蔬 + 1 米
INSERT INTO gift_box_product (gift_box_id, product_id, copies) VALUES
(3, 12, 1), -- 嘉義雞胸肉
(3, 1, 1),  -- 有機 A 菜
(3, 4, 1),  -- 空心菜
(3, 22, 1); -- 池上越光米

-- 早餐專屬箱 (gift_box_id = 4):奶蛋類
INSERT INTO gift_box_product (gift_box_id, product_id, copies) VALUES
(4, 19, 1), -- 文山牧場鮮奶
(4, 20, 1), -- 本土無毒土雞蛋
(4, 21, 1); -- 田媽媽鵪鶉蛋

-- 媽媽家常組合 (gift_box_id = 5):5 種家常食材
INSERT INTO gift_box_product (gift_box_id, product_id, copies) VALUES
(5, 14, 1), -- 本土豬絞肉
(5, 2, 1),  -- 有機高麗菜
(5, 9, 1),  -- 大甲牛蒡
(5, 20, 1), -- 本土無毒土雞蛋
(5, 22, 1); -- 池上越光米

-- 露營野炊組 (gift_box_id = 6):即食肉 + 雜糧
INSERT INTO gift_box_product (gift_box_id, product_id, copies) VALUES
(6, 11, 1), -- 雲林溫體豬五花
(6, 13, 1), -- 台南古早雞土雞腿
(6, 25, 1), -- 台南青仁黑豆
(6, 28, 1); -- 雲林豆瓣醬

-- ============================================================
-- 第六步:重置 group_buy 等資料
-- (group_buy_participant 跟 group_buy 已在第一步清掉)
-- ============================================================

ALTER TABLE group_buy AUTO_INCREMENT = 1;
ALTER TABLE group_buy_participant AUTO_INCREMENT = 1;
