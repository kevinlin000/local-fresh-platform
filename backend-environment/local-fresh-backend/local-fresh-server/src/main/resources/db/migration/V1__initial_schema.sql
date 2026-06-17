CREATE TABLE IF NOT EXISTS employee (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(32) NOT NULL,
  name VARCHAR(32) NOT NULL,
  password VARCHAR(64) NOT NULL,
  phone VARCHAR(11),
  sex VARCHAR(2),
  id_number VARCHAR(18),
  status INT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  create_user BIGINT,
  update_user BIGINT,
  UNIQUE KEY uk_employee_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type INT,
  name VARCHAR(32),
  sort INT,
  status INT,
  create_time DATETIME,
  update_time DATETIME,
  create_user BIGINT,
  update_user BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  openid VARCHAR(45),
  google_sub VARCHAR(64),
  email VARCHAR(128),
  name VARCHAR(32),
  phone VARCHAR(11),
  sex VARCHAR(2),
  id_number VARCHAR(18),
  avatar VARCHAR(500),
  avatar_url VARCHAR(512),
  login_provider VARCHAR(16) NOT NULL DEFAULT 'mock',
  create_time DATETIME,
  UNIQUE KEY uk_member_google_sub (google_sub)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_name VARCHAR(32) NOT NULL,
  category_id BIGINT,
  price DECIMAL(10, 2),
  image VARCHAR(255),
  description VARCHAR(255),
  status INT,
  create_time DATETIME,
  update_time DATETIME,
  create_user BIGINT,
  update_user BIGINT,
  INDEX idx_product_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_spec (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT,
  name VARCHAR(32),
  value VARCHAR(255),
  INDEX idx_product_spec_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gift_box (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT,
  box_name VARCHAR(32) NOT NULL,
  price DECIMAL(10, 2),
  status INT,
  description VARCHAR(255),
  image VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  create_user BIGINT,
  update_user BIGINT,
  INDEX idx_gift_box_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gift_box_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  gift_box_id BIGINT,
  product_id BIGINT,
  name VARCHAR(32),
  price DECIMAL(10, 2),
  copies INT,
  INDEX idx_gift_box_product_box (gift_box_id),
  INDEX idx_gift_box_product_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shipping_address (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id BIGINT,
  consignee VARCHAR(50),
  phone VARCHAR(11),
  sex VARCHAR(2),
  province_code VARCHAR(12),
  province_name VARCHAR(32),
  city_code VARCHAR(12),
  city_name VARCHAR(32),
  district_code VARCHAR(12),
  district_name VARCHAR(32),
  detail VARCHAR(255),
  label VARCHAR(32),
  is_default INT,
  INDEX idx_shipping_address_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  number VARCHAR(50),
  status INT,
  user_id BIGINT,
  address_book_id BIGINT,
  order_time DATETIME,
  checkout_time DATETIME,
  pay_method INT NOT NULL DEFAULT 1,
  pay_status INT,
  amount DECIMAL(10, 2),
  remark VARCHAR(100),
  user_name VARCHAR(32),
  phone VARCHAR(11),
  address VARCHAR(255),
  consignee VARCHAR(32),
  cancel_reason VARCHAR(255),
  rejection_reason VARCHAR(255),
  cancel_time DATETIME,
  estimated_delivery_time DATETIME,
  delivery_status INT NOT NULL DEFAULT 1,
  delivery_time DATETIME,
  pack_amount INT DEFAULT 0,
  tableware_number INT DEFAULT 0,
  tableware_status INT DEFAULT 0,
  INDEX idx_orders_user (user_id),
  INDEX idx_orders_number (number),
  INDEX idx_orders_status_time (status, order_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_detail (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(32),
  image VARCHAR(255),
  order_id BIGINT,
  product_id BIGINT,
  gift_box_id BIGINT,
  product_spec VARCHAR(10),
  number INT,
  amount DECIMAL(10, 2),
  INDEX idx_order_detail_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cart (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(32),
  user_id BIGINT,
  product_id BIGINT,
  gift_box_id BIGINT,
  product_spec VARCHAR(10),
  number INT,
  amount DECIMAL(10, 2),
  image VARCHAR(255),
  create_time DATETIME,
  INDEX idx_cart_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_buy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_no VARCHAR(50) NOT NULL,
  initiator_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  required_count INT NOT NULL DEFAULT 3,
  current_count INT NOT NULL DEFAULT 1,
  expire_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_group_buy_group_no (group_no),
  INDEX idx_status_expire (status, expire_at),
  INDEX idx_initiator (initiator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_buy_participant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_buy_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  pre_order_id BIGINT,
  joined_at DATETIME NOT NULL,
  UNIQUE KEY uk_group_member (group_buy_id, member_id),
  INDEX idx_group (group_buy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO employee (id, username, name, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user)
VALUES (1, 'admin', '管理員', 'e10adc3949ba59abbe56e057f20f883e', '0912345678', '1', 'A123456789', 1, NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE username = username;
