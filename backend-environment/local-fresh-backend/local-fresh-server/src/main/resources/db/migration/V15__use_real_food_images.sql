-- Use the project food photos instead of SVG category placeholders.
-- The files live under each frontend's public/demo-assets directory.

UPDATE product
SET image = CONCAT('/demo-assets/product-', id, '.png')
WHERE id BETWEEN 1 AND 29;

UPDATE gift_box
SET image = CONCAT('/demo-assets/gift-box-', id, '.png')
WHERE id BETWEEN 1 AND 6;

UPDATE cart
SET image = CASE
    WHEN product_id BETWEEN 1 AND 29 THEN CONCAT('/demo-assets/product-', product_id, '.png')
    WHEN gift_box_id BETWEEN 1 AND 6 THEN CONCAT('/demo-assets/gift-box-', gift_box_id, '.png')
    ELSE image
END
WHERE product_id BETWEEN 1 AND 29
   OR gift_box_id BETWEEN 1 AND 6;

UPDATE order_detail
SET image = CASE
    WHEN product_id BETWEEN 1 AND 29 THEN CONCAT('/demo-assets/product-', product_id, '.png')
    WHEN gift_box_id BETWEEN 1 AND 6 THEN CONCAT('/demo-assets/gift-box-', gift_box_id, '.png')
    ELSE image
END
WHERE product_id BETWEEN 1 AND 29
   OR gift_box_id BETWEEN 1 AND 6;
