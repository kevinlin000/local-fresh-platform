ALTER TABLE member
  ADD COLUMN password_hash VARCHAR(100) NULL AFTER phone,
  ADD UNIQUE KEY uk_member_email (email);
