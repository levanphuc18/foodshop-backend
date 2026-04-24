-- Add enabled column to users table
ALTER TABLE users ADD COLUMN enabled BOOLEAN DEFAULT TRUE;
