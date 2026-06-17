-- V6__add_product_images.sql
-- 將 V5 台灣化假資料的商品與直送箱圖片補上前端 public demo asset 路徑
-- 執行前提:V5__taiwan_localization.sql 已執行,且 product.id=1..29 / gift_box.id=1..6

UPDATE product
SET image = CASE id
    WHEN 1 THEN '/demo-assets/product-leaf.svg'
    WHEN 2 THEN '/demo-assets/product-leaf.svg'
    WHEN 3 THEN '/demo-assets/product-leaf.svg'
    WHEN 4 THEN '/demo-assets/product-leaf.svg'
    WHEN 5 THEN '/demo-assets/product-leaf.svg'
    WHEN 6 THEN '/demo-assets/product-root.svg'
    WHEN 7 THEN '/demo-assets/product-root.svg'
    WHEN 8 THEN '/demo-assets/product-root.svg'
    WHEN 9 THEN '/demo-assets/product-root.svg'
    WHEN 10 THEN '/demo-assets/product-root.svg'
    WHEN 11 THEN '/demo-assets/product-protein.svg'
    WHEN 12 THEN '/demo-assets/product-protein.svg'
    WHEN 13 THEN '/demo-assets/product-protein.svg'
    WHEN 14 THEN '/demo-assets/product-protein.svg'
    WHEN 15 THEN '/demo-assets/product-seafood.svg'
    WHEN 16 THEN '/demo-assets/product-seafood.svg'
    WHEN 17 THEN '/demo-assets/product-seafood.svg'
    WHEN 18 THEN '/demo-assets/product-seafood.svg'
    WHEN 19 THEN '/demo-assets/product-dairy.svg'
    WHEN 20 THEN '/demo-assets/product-dairy.svg'
    WHEN 21 THEN '/demo-assets/product-dairy.svg'
    WHEN 22 THEN '/demo-assets/product-grain.svg'
    WHEN 23 THEN '/demo-assets/product-grain.svg'
    WHEN 24 THEN '/demo-assets/product-grain.svg'
    WHEN 25 THEN '/demo-assets/product-grain.svg'
    WHEN 26 THEN '/demo-assets/product-condiment.svg'
    WHEN 27 THEN '/demo-assets/product-condiment.svg'
    WHEN 28 THEN '/demo-assets/product-condiment.svg'
    WHEN 29 THEN '/demo-assets/product-condiment.svg'
    ELSE image
END
WHERE id BETWEEN 1 AND 29;

UPDATE gift_box
SET image = CASE id
    WHEN 1 THEN '/demo-assets/gift-box.svg'
    WHEN 2 THEN '/demo-assets/gift-box.svg'
    WHEN 3 THEN '/demo-assets/gift-box.svg'
    WHEN 4 THEN '/demo-assets/gift-box.svg'
    WHEN 5 THEN '/demo-assets/gift-box.svg'
    WHEN 6 THEN '/demo-assets/gift-box.svg'
    ELSE image
END
WHERE id BETWEEN 1 AND 6;
