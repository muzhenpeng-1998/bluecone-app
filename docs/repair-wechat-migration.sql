-- =====================================================
-- Flyway Migration Repair Script
-- For V20251222001__create_wechat_openplatform_tables
-- =====================================================
--
-- This script repairs the failed Flyway migration by:
-- 1. Removing the failed migration record from flyway_schema_history
-- 2. The tables already exist, so the updated migration script
--    (which now uses CREATE TABLE IF NOT EXISTS) will succeed
--
-- Usage:
--   mysql -u root -p bluecone < repair-wechat-migration.sql
--
-- Or connect to your database and run:
--   DELETE FROM flyway_schema_history WHERE version = '20251222001';
--
-- =====================================================

USE bluecone;

-- Show current state
SELECT 'Current Flyway history for version 20251222001:' AS '';
SELECT version, description, type, installed_on, success 
FROM flyway_schema_history 
WHERE version = '20251222001';

-- Delete the failed migration record
DELETE FROM flyway_schema_history WHERE version = '20251222001';

-- Verify deletion
SELECT 'After cleanup:' AS '';
SELECT COUNT(*) AS remaining_records 
FROM flyway_schema_history 
WHERE version = '20251222001';

SELECT '✓ Flyway migration record removed successfully!' AS '';
SELECT 'You can now restart your application.' AS '';

