ALTER TABLE product_spec
  CHANGE COLUMN dish_id product_id BIGINT;

ALTER TABLE shipping_address
  CHANGE COLUMN user_id member_id BIGINT;
