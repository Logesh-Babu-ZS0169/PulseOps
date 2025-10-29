-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS inbound_config;

-- Create app_config table for storing application configuration
CREATE TABLE IF NOT EXISTS inbound_config.app_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50),
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on config_key for faster lookups
CREATE INDEX IF NOT EXISTS idx_app_config_key ON inbound_config.app_config(config_key);

-- Create index on category for filtering
CREATE INDEX IF NOT EXISTS idx_app_config_category ON inbound_config.app_config(category);

-- Add comments
COMMENT ON TABLE inbound_config.app_config IS 'Stores application configuration with database-level management';
COMMENT ON COLUMN inbound_config.app_config.config_key IS 'Unique configuration key (e.g., DB_HOST)';
COMMENT ON COLUMN inbound_config.app_config.config_value IS 'Configuration value';
COMMENT ON COLUMN inbound_config.app_config.description IS 'Human-readable description';
COMMENT ON COLUMN inbound_config.app_config.category IS 'Configuration category (database, email, scheduler, hikari, server)';
COMMENT ON COLUMN inbound_config.app_config.is_sensitive IS 'Whether the value is sensitive (password, API key)';
