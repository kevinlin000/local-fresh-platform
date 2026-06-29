-- Keep gift-box demo data self-explanatory for portfolio walkthroughs.
-- Existing V5 seed rows inserted gift_box_product without display names/prices.

UPDATE gift_box_product gbp
JOIN product p ON gbp.product_id = p.id
SET gbp.name = p.product_name,
    gbp.price = p.price
WHERE gbp.name IS NULL
   OR gbp.name = ''
   OR gbp.price IS NULL;

UPDATE gift_box
SET price = CASE id
    WHEN 1 THEN 340.00
    WHEN 2 THEN 620.00
    WHEN 3 THEN 460.00
    WHEN 4 THEN 270.00
    ELSE price
END
WHERE id IN (1, 2, 3, 4);
