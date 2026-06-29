-- Drop in reverse dependency order
DROP TABLE IF EXISTS group_buy_participant;
DROP TABLE IF EXISTS group_buy;
DROP TABLE IF EXISTS payment_event;
DROP TABLE IF EXISTS admin_operation_log;
DROP TABLE IF EXISTS product_inventory_log;
DROP TABLE IF EXISTS gift_box_product;
DROP TABLE IF EXISTS product_spec;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS shipping_address;
DROP TABLE IF EXISTS gift_box;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(32)  NOT NULL,
    name       VARCHAR(32)  NOT NULL,
    password   VARCHAR(64)  NOT NULL,
    phone      VARCHAR(11),
    sex        VARCHAR(2),
    id_number  VARCHAR(18),
    status     INT          DEFAULT 1,
    role       VARCHAR(16)  NOT NULL DEFAULT 'ADMIN',
    create_time DATETIME,
    update_time DATETIME,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        INT,
    name        VARCHAR(32),
    sort        INT,
    status      INT,
    create_time DATETIME,
    update_time DATETIME,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid      VARCHAR(45),
    google_sub  VARCHAR(64),
    email       VARCHAR(128),
    name        VARCHAR(32),
    phone       VARCHAR(11),
    password_hash VARCHAR(100),
    sex         VARCHAR(2),
    id_number   VARCHAR(18),
    avatar      VARCHAR(500),
    avatar_url  VARCHAR(512),
    login_provider VARCHAR(16) DEFAULT 'mock' NOT NULL,
    create_time DATETIME
);

CREATE UNIQUE INDEX uk_member_google_sub ON member (google_sub);
CREATE UNIQUE INDEX uk_member_email ON member (email);

CREATE TABLE product (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(32)   NOT NULL,
    category_id  BIGINT,
    price        DECIMAL(10, 2),
    image        VARCHAR(255),
    description  VARCHAR(255),
    status       INT,
    stock        INT DEFAULT 100 NOT NULL,
    low_stock_threshold INT DEFAULT 10 NOT NULL,
    create_time  DATETIME,
    update_time  DATETIME,
    create_user  BIGINT,
    update_user  BIGINT
);

CREATE TABLE product_spec (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT,
    name       VARCHAR(32),
    value      VARCHAR(255)
);

CREATE TABLE product_inventory_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT      NOT NULL,
    change_quantity INT         NOT NULL,
    stock_before    INT         NOT NULL,
    stock_after     INT         NOT NULL,
    reason          VARCHAR(64) NOT NULL,
    remark          VARCHAR(255),
    reference_type  VARCHAR(32),
    reference_id    BIGINT,
    operator_type   VARCHAR(32) NOT NULL,
    operator_id     BIGINT,
    idempotency_key VARCHAR(255),
    created_at      DATETIME    NOT NULL
);

CREATE UNIQUE INDEX uk_inventory_log_idempotency_key
    ON product_inventory_log (idempotency_key);

CREATE TABLE admin_operation_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    action        VARCHAR(64) NOT NULL,
    target_type   VARCHAR(32) NOT NULL,
    target_id     BIGINT      NOT NULL,
    before_value  VARCHAR(64),
    after_value   VARCHAR(64),
    reason        VARCHAR(255),
    operator_type VARCHAR(32) NOT NULL,
    operator_id   BIGINT,
    created_at    DATETIME    NOT NULL
);

CREATE TABLE payment_event (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id           BIGINT,
    order_number       VARCHAR(50) NOT NULL,
    provider           VARCHAR(32) NOT NULL,
    event_type         VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(255),
    provider_trade_no  VARCHAR(128),
    idempotency_key    VARCHAR(255),
    amount             DECIMAL(10, 2),
    result             VARCHAR(32) NOT NULL,
    raw_payload        TEXT,
    created_at         DATETIME    NOT NULL
);

CREATE TABLE gift_box (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    box_name    VARCHAR(32)   NOT NULL,
    price       DECIMAL(10, 2),
    status      INT,
    description VARCHAR(255),
    image       VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE gift_box_product (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    gift_box_id BIGINT,
    product_id  BIGINT,
    name        VARCHAR(32),
    price       DECIMAL(10, 2),
    copies      INT
);

CREATE TABLE shipping_address (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT,
    consignee     VARCHAR(50),
    phone         VARCHAR(11),
    sex           VARCHAR(2),
    province_code VARCHAR(12),
    province_name VARCHAR(32),
    city_code     VARCHAR(12),
    city_name     VARCHAR(32),
    district_code VARCHAR(12),
    district_name VARCHAR(32),
    detail        VARCHAR(255),
    label         VARCHAR(32),
    is_default    INT
);

CREATE TABLE orders (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    number                   VARCHAR(50),
    status                   INT,
    user_id                  BIGINT,
    address_book_id          BIGINT,
    order_time               DATETIME,
    checkout_time            DATETIME,
    pay_method               INT          NOT NULL DEFAULT 1,
    pay_status               INT,
    amount                   DECIMAL(10, 2),
    remark                   VARCHAR(100),
    user_name                VARCHAR(32),
    phone                    VARCHAR(11),
    address                  VARCHAR(255),
    consignee                VARCHAR(32),
    cancel_reason            VARCHAR(255),
    rejection_reason         VARCHAR(255),
    cancel_time              DATETIME,
    estimated_delivery_time  DATETIME,
    delivery_status          INT          NOT NULL DEFAULT 1,
    delivery_time            DATETIME,
    pack_amount              INT          DEFAULT 0,
    tableware_number         INT          DEFAULT 0,
    tableware_status         INT          DEFAULT 0
);

CREATE TABLE order_detail (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(32),
    image       VARCHAR(255),
    order_id    BIGINT,
    product_id  BIGINT,
    gift_box_id BIGINT,
    product_spec VARCHAR(10),
    number      INT,
    amount      DECIMAL(10, 2)
);

CREATE TABLE cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(32),
    user_id     BIGINT,
    product_id  BIGINT,
    gift_box_id BIGINT,
    product_spec VARCHAR(10),
    number      INT,
    amount      DECIMAL(10, 2),
    image       VARCHAR(255),
    create_time DATETIME
);

CREATE TABLE group_buy (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_no       VARCHAR(50) NOT NULL UNIQUE,
    initiator_id   BIGINT      NOT NULL,
    status         TINYINT     NOT NULL DEFAULT 1,
    required_count INT         NOT NULL DEFAULT 3,
    current_count  INT         NOT NULL DEFAULT 1,
    expire_at      DATETIME    NOT NULL,
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME    NOT NULL
);

CREATE INDEX idx_status_expire ON group_buy (status, expire_at);
CREATE INDEX idx_initiator ON group_buy (initiator_id);

CREATE TABLE group_buy_participant (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_buy_id BIGINT   NOT NULL,
    member_id    BIGINT   NOT NULL,
    pre_order_id BIGINT,
    joined_at    DATETIME NOT NULL,
    CONSTRAINT uk_group_member UNIQUE (group_buy_id, member_id)
);

CREATE INDEX idx_group ON group_buy_participant (group_buy_id);
