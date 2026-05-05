ALTER TABLE member
  ADD COLUMN google_sub VARCHAR(64) UNIQUE COMMENT 'Google OAuth sub claim',
  ADD COLUMN email VARCHAR(128) COMMENT '電子郵件',
  ADD COLUMN avatar_url VARCHAR(512) COMMENT 'Google 頭像 URL',
  ADD COLUMN login_provider VARCHAR(16) NOT NULL DEFAULT 'mock' COMMENT '登入方式: mock / google';
