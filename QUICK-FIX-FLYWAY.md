# Quick Fix for Flyway Migration Error

## The Problem

Your application won't start because Flyway detected a failed migration:

```
Detected failed migration to version 20251218001 (add payment notify id).
```

## The Solution (2 Steps)

### Step 1: Run the Repair Tool

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app

mvn spring-boot:run -Dspring-boot.run.profiles=local,repair -Dspring-boot.run.arguments="--repair.migration.version=20251218001"
```

**What this does:**
- Removes any partial database changes from the failed migration
- Cleans up the Flyway history table
- Prepares the database for a clean re-run of the migration

**Expected output:**
```
=================================================================
Flyway Migration Repair Tool
=================================================================

Repairing migration version: 20251218001
-----------------------------------------------------------------

[Step 1] Checking current database state...
   ✓ Column bc_payment_notify_log.notify_id exists
   ...

[Step 2] Rolling back partial changes...
   ✓ Dropped index bc_payment_notify_log.uk_notify_id
   ✓ Dropped column bc_payment_notify_log.notify_id
   ...

[Step 3] Cleaning up Flyway schema history...
   ✓ Deleted 1 record(s) from flyway_schema_history

[Step 4] Verifying cleanup...
   ✓ Flyway history cleaned successfully
   ✓ Column bc_payment_notify_log.notify_id removed
   ...

=================================================================
✓ Repair completed successfully!
=================================================================
```

### Step 2: Restart Your Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway will now automatically re-run the migration cleanly, and your application should start successfully!

## Troubleshooting

### If the repair tool fails to start

**Problem:** Maven can't find dependencies or the application won't start

**Solution:** Make sure you're in the correct directory and have the right profile:
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local,repair -Dspring-boot.run.arguments="--repair.migration.version=20251218001"
```

### If the application still fails after repair

**Problem:** The migration fails again with a different error

**Solution:** Check the migration script for issues:
1. Open `app-infra/src/main/resources/db/migration/V20251218001__add_payment_notify_id.sql`
2. Verify that the tables `bc_payment_notify_log` and `bc_order` exist in your database
3. Check if there are any data issues (e.g., duplicate values in `notify_id`)

## Need More Help?

See the comprehensive guide: [FLYWAY-MIGRATION-REPAIR-GUIDE.md](./FLYWAY-MIGRATION-REPAIR-GUIDE.md)

---

**TL;DR:** Run the repair tool, then restart your app. Done! 🎉
