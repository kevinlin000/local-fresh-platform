RENAME TABLE
  user TO member,
  dish TO product,
  dish_flavor TO product_spec,
  setmeal TO gift_box,
  setmeal_dish TO gift_box_product,
  shopping_cart TO cart,
  address_book TO shipping_address;

ALTER TABLE product
  CHANGE COLUMN name product_name VARCHAR(32) NOT NULL;

ALTER TABLE gift_box
  CHANGE COLUMN name box_name VARCHAR(32) NOT NULL;

ALTER TABLE gift_box_product
  CHANGE COLUMN setmeal_id gift_box_id BIGINT,
  CHANGE COLUMN dish_id product_id BIGINT;

ALTER TABLE cart
  CHANGE COLUMN dish_id product_id BIGINT,
  CHANGE COLUMN setmeal_id gift_box_id BIGINT,
  CHANGE COLUMN dish_flavor product_spec VARCHAR(10);

ALTER TABLE order_detail
  CHANGE COLUMN dish_id product_id BIGINT,
  CHANGE COLUMN setmeal_id gift_box_id BIGINT,
  CHANGE COLUMN dish_flavor product_spec VARCHAR(10);
