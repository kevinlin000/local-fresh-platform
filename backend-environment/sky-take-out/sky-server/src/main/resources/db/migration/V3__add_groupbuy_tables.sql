CREATE TABLE group_buy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_no VARCHAR(50) NOT NULL UNIQUE COMMENT '揪團編號（Snowflake）',
  initiator_id BIGINT NOT NULL COMMENT '發起人 member_id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=揪團中 2=已成團 3=已失敗 4=已完成',
  required_count INT NOT NULL DEFAULT 3 COMMENT '成團人數',
  current_count INT NOT NULL DEFAULT 1 COMMENT '當前人數',
  expire_at DATETIME NOT NULL COMMENT '揪團截止時間',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_status_expire (status, expire_at),
  INDEX idx_initiator (initiator_id)
);

CREATE TABLE group_buy_participant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_buy_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  pre_order_id BIGINT COMMENT '預訂單 ID（成團後生效，揪團期間 status=PENDING）',
  joined_at DATETIME NOT NULL,
  UNIQUE KEY uk_group_member (group_buy_id, member_id) COMMENT '同一人不能重複加團',
  INDEX idx_group (group_buy_id)
);
