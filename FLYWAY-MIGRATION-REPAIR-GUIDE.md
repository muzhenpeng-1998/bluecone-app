# Flyway Migration Repair Guide

## Problem Summary

**Error:** `FlywayValidateException: Validate failed: Migrations have failed validation`

**Failed Migration:** `V20251218001__add_payment_notify_id.sql`

**Root Cause:** The migration `V20251218001` failed during execution, leaving the database in an inconsistent state. Flyway marks it as "failed" in the `flyway_schema_history` table and refuses to proceed with any further migrations or application startup.

## Error Details

```
Detected failed migration to version 20251218001 (add payment notify id).
Please remove any half-completed changes then run repair to fix the schema history.
```

This error occurs when:
1. A migration script starts executing
2. One or more SQL statements succeed
3. A subsequent SQL statement fails (e.g., due to syntax error, constraint violation, or missing table)
4. Flyway marks the migration as "failed" with `success = false` in `flyway_schema_history`
5. The database is left with partial changes from the migration

## Solution Options

### Option 1: Java-Based Repair Tool (Recommended - No MySQL Client Required)

Use the provided Java repair tool that works directly with your Spring Boot application:

```bash
# Run the repair tool
mvn spring-boot:run -Dspring-boot.run.profiles=local,repair -Dspring-boot.run.arguments="--repair.migration.version=20251218001"
```

The tool will:
1. ✅ Check current database state
2. ✅ Rollback any partial changes from the failed migration
3. ✅ Clean up the Flyway schema history
4. ✅ Verify the cleanup was successful

After running the tool, restart your application normally and Flyway will automatically re-run the migration cleanly.

**Advantages:**
- ✅ No need to install MySQL client
- ✅ Uses your existing database configuration
- ✅ Safe and automated
- ✅ Provides detailed progress output

### Option 2: Bash Script Repair (Requires MySQL Client)

Use the provided bash script to automatically fix the issue:

```bash
# Run the repair script
./fix-flyway.sh

# If you need to specify custom database credentials:
DB_HOST=localhost DB_PORT=3306 DB_NAME=bluecone DB_USERNAME=root DB_PASSWORD=yourpass ./fix-flyway.sh
```

The script will:
1. ✅ Check current database state
2. ✅ Rollback any partial changes from the failed migration
3. ✅ Clean up the Flyway schema history
4. ✅ Verify the cleanup was successful

After running the script, restart your application and Flyway will automatically re-run the migration cleanly.

**Note:** This option requires the MySQL command-line client to be installed.

### Option 3: Manual Repair via SQL

If you prefer to fix it manually or the script doesn't work, follow these steps:

#### Step 1: Connect to your database

```bash
mysql -h localhost -P 3306 -u root -p bluecone
```

#### Step 2: Check what was applied

```sql
-- Check if notify_id column exists
SELECT TABLE_NAME, COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND COLUMN_NAME = 'notify_id';

-- Check if close_reason and closed_at columns exist
SELECT TABLE_NAME, COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME IN ('close_reason', 'closed_at');

-- Check if unique index exists
SELECT TABLE_NAME, INDEX_NAME 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND INDEX_NAME = 'uk_notify_id';

-- Check Flyway history
SELECT * FROM flyway_schema_history 
WHERE version = '20251218001';
```

#### Step 3: Rollback partial changes

Based on what you found in Step 2, run the appropriate rollback commands:

```sql
-- Drop unique index if it exists
ALTER TABLE bc_payment_notify_log DROP INDEX uk_notify_id;

-- Drop notify_id column if it exists
ALTER TABLE bc_payment_notify_log DROP COLUMN notify_id;

-- Drop close_reason column if it exists
ALTER TABLE bc_order DROP COLUMN close_reason;

-- Drop closed_at column if it exists
ALTER TABLE bc_order DROP COLUMN closed_at;
```

**Note:** If a column or index doesn't exist, MySQL will throw an error. That's okay - just skip that statement and continue with the next one.

#### Step 4: Clean up Flyway schema history

```sql
-- Remove the failed migration record
DELETE FROM flyway_schema_history 
WHERE version = '20251218001';
```

#### Step 5: Verify cleanup

```sql
-- Verify columns are removed
SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND COLUMN_NAME = 'notify_id';
-- Should return 0

SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME IN ('close_reason', 'closed_at');
-- Should return 0

-- Verify Flyway history is cleaned
SELECT COUNT(*) FROM flyway_schema_history 
WHERE version = '20251218001';
-- Should return 0
```

#### Step 6: Restart your application

```bash
# Stop your application if it's running
# Then start it again
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway will automatically detect that migration `V20251218001` hasn't been applied and will run it cleanly.

### Option 4: Use Flyway Repair Command (If Flyway CLI is available)

If you have Flyway CLI installed, you can use the repair command:

```bash
flyway repair -url=jdbc:mysql://localhost:3306/bluecone -user=root -password=yourpass
```

However, this only marks the migration as fixed in the history table - **you still need to manually rollback any partial database changes** from Step 3 above.

## Understanding the Failed Migration

The migration `V20251218001__add_payment_notify_id.sql` was attempting to:

1. ✅ Add `notify_id` column to `bc_payment_notify_log` table
2. ❌ Create unique index `uk_notify_id` on `notify_id` column (likely failed here)
3. ❓ Add `close_reason` column to `bc_order` table (may or may not have executed)
4. ❓ Add `closed_at` column to `bc_order` table (may or may not have executed)

The most common reasons for failure:
- **Duplicate values:** If the `notify_id` column was added but contains duplicate or NULL values, creating a unique index will fail
- **Table doesn't exist:** If `bc_payment_notify_log` or `bc_order` tables don't exist
- **Syntax error:** SQL syntax incompatibility with your MySQL version
- **Permissions:** Database user lacks ALTER TABLE privileges

## Prevention Tips

To avoid similar issues in the future:

1. **Test migrations locally first:** Always test new migration scripts on a local database before deploying
2. **Use transactions carefully:** MySQL DDL statements (ALTER TABLE, CREATE INDEX) are not transactional and cannot be rolled back automatically
3. **Check data before adding constraints:** Before adding UNIQUE indexes, verify there are no duplicate values
4. **Use conditional DDL:** Use `IF NOT EXISTS` clauses where possible (though MySQL has limited support)
5. **Split complex migrations:** Break large migrations into smaller, atomic changes
6. **Enable Flyway callbacks:** Use Flyway callbacks to validate data before applying constraints

## Troubleshooting

### Issue: Script fails with "mysql: command not found"

**Solution:** Install MySQL client:
```bash
# macOS
brew install mysql-client

# Ubuntu/Debian
sudo apt-get install mysql-client

# CentOS/RHEL
sudo yum install mysql
```

### Issue: Script fails with "Access denied"

**Solution:** Check your database credentials:
```bash
# Test connection manually
mysql -h localhost -P 3306 -u root -p bluecone

# If that works, run the script with explicit credentials
DB_USERNAME=root DB_PASSWORD=yourpass ./fix-flyway.sh
```

### Issue: Application still fails after repair

**Solution:** 
1. Verify the repair was successful by checking the database state (see Step 2 above)
2. Check if there are other failed migrations: `SELECT * FROM flyway_schema_history WHERE success = 0;`
3. Clear Maven/Spring Boot cache: `mvn clean install`
4. Check application logs for other errors

### Issue: Migration fails again after repair

**Solution:** The migration script itself may have an issue. Check:
1. Does `bc_payment_notify_log` table exist?
2. Does `bc_order` table exist?
3. Are there duplicate values in `notify_id` column?
4. Review the migration script for syntax errors

## Files Provided

1. **`app-application/src/main/java/com/bluecone/app/migration/FlywayRepairTool.java`** - Java-based repair tool (recommended)
2. **`app-application/src/main/resources/application-repair.yml`** - Configuration for repair profile
3. **`fix-flyway.sh`** - Bash script for repair (requires MySQL client)
4. **`FIX-FLYWAY-FAILED-MIGRATION.sql`** - SQL-only repair script for manual execution
5. **`FLYWAY-MIGRATION-REPAIR-GUIDE.md`** - This comprehensive guide

## Quick Start (Recommended)

```bash
# Run the Java-based repair tool
mvn spring-boot:run -Dspring-boot.run.profiles=local,repair -Dspring-boot.run.arguments="--repair.migration.version=20251218001"

# After successful repair, restart your application normally
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Alternative Quick Start (Requires MySQL Client)

```bash
# 1. Make the script executable (if not already)
chmod +x fix-flyway.sh

# 2. Run the repair script
./fix-flyway.sh

# 3. Restart your application
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Additional Resources

- [Flyway Documentation - Repair](https://documentation.red-gate.com/fd/repair-184127457.html)
- [Flyway Documentation - Validate](https://documentation.red-gate.com/fd/validate-184127461.html)
- [MySQL ALTER TABLE Documentation](https://dev.mysql.com/doc/refman/8.0/en/alter-table.html)

## Support

If you continue to experience issues after following this guide:

1. Check the application logs for detailed error messages
2. Verify database connectivity: `mysql -h localhost -P 3306 -u root -p bluecone`
3. Review the migration script: `app-infra/src/main/resources/db/migration/V20251218001__add_payment_notify_id.sql`
4. Check Flyway configuration: `app-application/src/main/resources/application.yml`

---

**Last Updated:** 2025-12-18  
**Version:** 1.0
