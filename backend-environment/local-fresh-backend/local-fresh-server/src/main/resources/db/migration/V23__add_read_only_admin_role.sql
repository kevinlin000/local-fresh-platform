ALTER TABLE employee
  ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'ADMIN';

UPDATE employee
SET role = 'ADMIN'
WHERE role IS NULL OR role = '';

INSERT INTO employee
    (username, name, password, phone, sex, id_number, status, role, create_time, update_time, create_user, update_user)
VALUES
    ('demo_viewer', '面試展示帳號', 'b48d9c5f3bc873d3213500c1a0c5eadd', '0900000000', '1', 'A000000000', 1, 'VIEWER', NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    password = VALUES(password),
    status = VALUES(status),
    role = VALUES(role),
    update_time = NOW();
