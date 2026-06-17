-- V6__add_product_images.sql
-- 將 V5 台灣化假資料的商品與直送箱圖片補上 S3 URL
-- 執行前提:V5__taiwan_localization.sql 已執行,且 product.id=1..29 / gift_box.id=1..6

UPDATE product
SET image = CASE id
    WHEN 1 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-1.png'
    WHEN 2 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-2.png'
    WHEN 3 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-3.png'
    WHEN 4 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-4.png'
    WHEN 5 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-5.png'
    WHEN 6 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-6.png'
    WHEN 7 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-7.png'
    WHEN 8 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-8.png'
    WHEN 9 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-9.png'
    WHEN 10 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-10.png'
    WHEN 11 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-11.png'
    WHEN 12 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-12.png'
    WHEN 13 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-13.png'
    WHEN 14 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-14.png'
    WHEN 15 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-15.png'
    WHEN 16 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-16.png'
    WHEN 17 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-17.png'
    WHEN 18 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-18.png'
    WHEN 19 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-19.png'
    WHEN 20 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-20.png'
    WHEN 21 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-21.png'
    WHEN 22 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-22.png'
    WHEN 23 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-23.png'
    WHEN 24 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-24.png'
    WHEN 25 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-25.png'
    WHEN 26 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-26.png'
    WHEN 27 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-27.png'
    WHEN 28 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-28.png'
    WHEN 29 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/product-29.png'
    ELSE image
END
WHERE id BETWEEN 1 AND 29;

UPDATE gift_box
SET image = CASE id
    WHEN 1 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/gift-box-1.png'
    WHEN 2 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/gift-box-2.png'
    WHEN 3 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/gift-box-3.png'
    WHEN 4 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/gift-box-4.png'
    WHEN 5 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/gift-box-5.png'
    WHEN 6 THEN 'https://sky-takeout-kevin-2026.s3.ap-northeast-1.amazonaws.com/products/gift-box-6.png'
    ELSE image
END
WHERE id BETWEEN 1 AND 6;
