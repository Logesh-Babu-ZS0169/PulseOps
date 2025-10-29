-- Create users table for authentication
CREATE TABLE IF NOT EXISTS intics.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Insert default admin user (password: admin123)
INSERT INTO intics.users (username, email, password, full_name, is_active)
VALUES (
    'admin',
    'admin@intics.com',
    '$2a$10$rqQw8VhKz.6FZNqGp3EKAuBLvMXL3QH5Fq.HJKzLvYu1vKCxXQKqu',
    'System Administrator',
    TRUE
) ON CONFLICT (username) DO NOTHING;
