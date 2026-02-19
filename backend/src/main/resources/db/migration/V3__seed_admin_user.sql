-- Seed initial admin user
-- Password is 'password' (BCrypt hash)
INSERT INTO users (email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES 
('admin@orthowatch.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgV5MhKZQ.O2.MpxXFm/im.7O', 'Admin User', 'ADMIN', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
