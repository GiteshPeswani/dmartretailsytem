-- V2__seed_roles_and_admin.sql
-- Insert default roles

INSERT IGNORE INTO roles (name) VALUES ('CUSTOMER'), ('ADMIN'), ('STORE_MANAGER');

-- Insert default admin user (password: Admin@123 — BCrypt hash)
-- Change via environment variable ADMIN_PASSWORD_HASH or update after first boot
INSERT IGNORE INTO users (first_name, last_name, email, password_hash, phone, enabled)
VALUES (
    'System',
    'Admin',
    'admin@dmart.local',
    '$2a$12$RlWLyxDp0Eu8sCxO2Eq9l.q.4dMM3Yl35m6NXTMKLqnGhJKS0NG2',
    '0000000000',
    TRUE
);

-- Assign ADMIN role to admin user
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@dmart.local' AND r.name = 'ADMIN';

-- Insert default store manager (password: Manager@123)
INSERT IGNORE INTO users (first_name, last_name, email, password_hash, phone, enabled)
VALUES (
    'Store',
    'Manager',
    'manager@dmart.local',
    '$2a$12$Qn3EJzQrVTKqWqWqWqWqWuqZzKq3EJzQrVTKqWqWqWqWqWuqZzKq3',
    '1111111111',
    TRUE
);

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'manager@dmart.local' AND r.name = 'STORE_MANAGER';
