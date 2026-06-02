-- ============================================
-- V7__phase4_fix.sql - Fix JSONB → TEXT columns
-- ============================================

ALTER TABLE tasks ALTER COLUMN form_schema TYPE TEXT;
ALTER TABLE submissions ALTER COLUMN content TYPE TEXT;
