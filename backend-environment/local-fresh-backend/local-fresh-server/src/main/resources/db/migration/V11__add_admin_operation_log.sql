CREATE TABLE IF NOT EXISTS admin_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  action VARCHAR(64) NOT NULL COMMENT '管理端操作類型',
  target_type VARCHAR(32) NOT NULL COMMENT '操作目標類型',
  target_id BIGINT NOT NULL COMMENT '操作目標 id',
  before_value VARCHAR(64) COMMENT '操作前狀態或值',
  after_value VARCHAR(64) COMMENT '操作後狀態或值',
  reason VARCHAR(255) COMMENT '操作原因',
  operator_type VARCHAR(32) NOT NULL COMMENT '操作者類型',
  operator_id BIGINT COMMENT '操作者 id',
  created_at DATETIME NOT NULL COMMENT '操作時間',
  INDEX idx_admin_operation_target_time (target_type, target_id, created_at),
  INDEX idx_admin_operation_operator_time (operator_type, operator_id, created_at)
) COMMENT='管理端操作審計紀錄';
