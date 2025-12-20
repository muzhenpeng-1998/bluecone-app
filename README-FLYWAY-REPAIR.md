# Flyway Migration Repair Resources

## 🚨 Your Application Won't Start?

If you're seeing this error:
```
FlywayValidateException: Validate failed: Migrations have failed validation
Detected failed migration to version 20251218001 (add payment notify id).
```

**You're in the right place!** This guide will help you fix it.

---

## 🎯 Quick Fix (Recommended)

Run this **one command** to fix everything:

```bash
./repair-and-restart.sh
```

That's it! The script will:
1. ✅ Repair the database automatically
2. ✅ Restart your application
3. ✅ Let Flyway re-run the migration cleanly

---

## 📚 Documentation Guide

Choose the right document for your needs:

### 🏃 Just Want to Fix It Fast?
**Read:** [`QUICK-FIX-FLYWAY.md`](./QUICK-FIX-FLYWAY.md)
- 2-step fix process
- Takes 2 minutes
- No technical details, just the solution

### 🤔 Want to Understand What Happened?
**Read:** [`FLYWAY-REPAIR-SUMMARY.md`](./FLYWAY-REPAIR-SUMMARY.md)
- Explains the problem
- Shows what tools were created
- Describes how the repair works
- Includes troubleshooting tips

### 🔧 Need Detailed Troubleshooting?
**Read:** [`FLYWAY-MIGRATION-REPAIR-GUIDE.md`](./FLYWAY-MIGRATION-REPAIR-GUIDE.md)
- Comprehensive guide
- Multiple repair options
- Step-by-step manual instructions
- Advanced troubleshooting
- Prevention tips

---

## 🛠️ Available Tools

### Option 1: One-Command Script (Easiest)
```bash
./repair-and-restart.sh
```
**What it does:** Runs repair + restarts app automatically

### Option 2: Java Repair Tool (Recommended)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,repair \
  -Dspring-boot.run.arguments="--repair.migration.version=20251218001"
```
**What it does:** Uses Spring Boot to repair the database  
**Advantages:** No external tools needed, works with your existing config

### Option 3: Bash Script (If you have MySQL client)
```bash
./fix-flyway.sh
```
**What it does:** Connects to MySQL and runs repair SQL  
**Requires:** MySQL command-line client installed

### Option 4: Manual SQL (For experts)
```bash
mysql -h localhost -P 3306 -u root -p bluecone < FIX-FLYWAY-FAILED-MIGRATION.sql
```
**What it does:** Manually execute repair SQL  
**When to use:** If automated tools don't work

---

## 📁 File Reference

| File | Purpose | Type |
|------|---------|------|
| **Documentation** | | |
| `README-FLYWAY-REPAIR.md` | This file - navigation guide | 📄 Doc |
| `QUICK-FIX-FLYWAY.md` | Quick 2-step fix | 📄 Doc |
| `FLYWAY-REPAIR-SUMMARY.md` | Problem explanation & summary | 📄 Doc |
| `FLYWAY-MIGRATION-REPAIR-GUIDE.md` | Comprehensive troubleshooting | 📄 Doc |
| **Automated Tools** | | |
| `repair-and-restart.sh` | One-command fix script | 🔧 Script |
| `FlywayRepairTool.java` | Java-based repair tool | ⚙️ Code |
| `application-repair.yml` | Repair profile config | ⚙️ Config |
| `fix-flyway.sh` | Bash repair script | 🔧 Script |
| **Manual Tools** | | |
| `FIX-FLYWAY-FAILED-MIGRATION.sql` | SQL repair script | 📝 SQL |

---

## 🎓 Understanding the Problem

### What Happened?
1. Migration `V20251218001` started executing
2. Some SQL statements succeeded (added columns)
3. A later statement failed (likely the unique index)
4. Database left in inconsistent state
5. Flyway marked migration as "failed"
6. Application won't start

### What the Repair Does
1. ✅ Checks what was partially applied
2. ✅ Rolls back partial changes
3. ✅ Cleans Flyway history table
4. ✅ Allows clean re-run of migration

### Why It's Safe
- Only removes changes from the failed migration
- Doesn't touch other data or tables
- Can be run multiple times safely
- Fully reversible

---

## 🚀 Getting Started

### First Time Here?

1. **Quick fix:** Run `./repair-and-restart.sh`
2. **If that fails:** Read `QUICK-FIX-FLYWAY.md`
3. **Still stuck?** Read `FLYWAY-MIGRATION-REPAIR-GUIDE.md`

### Want to Understand First?

1. **Read:** `FLYWAY-REPAIR-SUMMARY.md` (5 min read)
2. **Then run:** `./repair-and-restart.sh`
3. **Success!** Your app should start

---

## ❓ FAQ

### Q: Is this safe to run?
**A:** Yes! The repair only removes changes from the failed migration. It doesn't touch your data.

### Q: Can I run it multiple times?
**A:** Yes! The repair is idempotent - safe to run multiple times.

### Q: What if it fails?
**A:** Check `FLYWAY-MIGRATION-REPAIR-GUIDE.md` for detailed troubleshooting.

### Q: Will I lose data?
**A:** No! The repair only removes empty columns that were just added. No data is deleted.

### Q: Which tool should I use?
**A:** Use `repair-and-restart.sh` - it's the easiest and most reliable.

### Q: Do I need MySQL client installed?
**A:** No! The Java repair tool (`repair-and-restart.sh`) works without it.

---

## 🆘 Still Need Help?

### If the repair fails:
1. Check the error message carefully
2. Read `FLYWAY-MIGRATION-REPAIR-GUIDE.md` troubleshooting section
3. Verify database connectivity: `mysql -h localhost -P 3306 -u root -p bluecone`
4. Check if tables exist: `SHOW TABLES LIKE 'bc_%';`

### If the migration fails again after repair:
1. The migration script may have an issue
2. Check: `app-infra/src/main/resources/db/migration/V20251218001__add_payment_notify_id.sql`
3. Verify tables exist: `bc_payment_notify_log`, `bc_order`
4. Check for duplicate data in `notify_id` column

---

## 📞 Quick Reference

### Run Repair
```bash
./repair-and-restart.sh
```

### Check Database
```bash
mysql -h localhost -P 3306 -u root -p bluecone
```

### View Flyway History
```sql
SELECT * FROM flyway_schema_history 
WHERE version = '20251218001';
```

### Check Application Logs
```bash
tail -f logs/application.log
```

---

## ✅ Success Checklist

After running the repair, you should see:

- [ ] Repair tool completes without errors
- [ ] All columns/indexes rolled back successfully
- [ ] Flyway history cleaned
- [ ] Application starts successfully
- [ ] Migration `V20251218001` shows as completed in logs
- [ ] No more Flyway validation errors

---

**Created:** 2025-12-18  
**Last Updated:** 2025-12-18  
**Version:** 1.0  
**Status:** Ready to use ✅
