-- Seed surgeon user for enrollment testing
-- Password is 'password' (BCrypt hash with 12 rounds)
INSERT INTO users (email, password_hash, full_name, role, phone, is_active, created_at, updated_at)
VALUES
('surgeon@orthowatch.com', '$2a$12$LJ3a4FnMBMih8RpMYh8WSOyZT3UMmnKlx6JMk5YQ66XqHf5MJhqHK', 'Dr. Ramesh Kumar', 'SURGEON', '+919876543210', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
