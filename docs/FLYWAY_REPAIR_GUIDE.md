# Flyway Migration Repair Guide

## Problem

The Flyway migration `V20251222001__create_wechat_openplatform_tables.sql` failed because the WeChat tables already existed in the database. This left a failed migration record in the `flyway_schema_history` table, preventing the application from starting.

## Solution

Two issues have been fixed:

### 1. Fixed Spring Boot Auto-Configuration Issue
- **Problem**: `WxJavaOpenConfiguration` class was referenced but didn't exist
- **Fix**: Removed the non-existent class reference from `app-wechat/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### 2. Fixed Flyway Migration Script
- **Problem**: Migration tried to create tables that already existed
- **Fix**: Updated migration script to use `CREATE TABLE IF NOT EXISTS`

## Steps to Repair

### Option 1: Using the SQL Repair Script (Recommended)

Run the provided SQL script to clean up the failed migration record:

```bash
# Connect to your MySQL database and run:
mysql -u root -p bluecone < repair-wechat-migration.sql
```

Or manually execute:

```sql
DELETE FROM flyway_schema_history WHERE version = '20251222001';
```

### Option 2: Using the Java Repair Tool

If you need to customize the repair process:

```bash
# Edit SimpleFlywayRepair.java to match your database connection settings
# Then compile and run:
javac -cp "$(find ~/.m2/repository/com/mysql/mysql-connector-j -name '*.jar' | head -1)" SimpleFlywayRepair.java
java -cp ".:$(find ~/.m2/repository/com/mysql/mysql-connector-j -name '*.jar' | head -1)" SimpleFlywayRepair
```

### Option 3: Using the Built-in Flyway Repair Tool

```bash
mvn spring-boot:run -pl app-application -am \
  -Dspring-boot.run.profiles=local,repair \
  -Dspring-boot.run.mainClass=com.bluecone.app.migration.FlywayRepairTool \
  -Dspring-boot.run.arguments="--repair.migration.version=20251222001"
```

## After Repair

Once the failed migration record is removed:

1. **Restart your application**:
   ```bash
   mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
   ```

2. **Flyway will automatically re-run the migration** with the fixed script that uses `CREATE TABLE IF NOT EXISTS`

3. **The migration should complete successfully** since the tables already exist and the script is now idempotent

## Verification

Check that the migration completed successfully:

```sql
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
WHERE version = '20251222001';
```

You should see `success = 1`.

## Files Modified

1. `app-wechat/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - Removed reference to non-existent `WxJavaOpenConfiguration`

2. `app-infra/src/main/resources/db/migration/V20251222001__create_wechat_openplatform_tables.sql`
   - Changed `CREATE TABLE` to `CREATE TABLE IF NOT EXISTS` for both tables

3. `app-infra/src/main/java/com/bluecone/app/infra/config/MybatisPlusConfig.java`
   - Fixed MyBatis mapper scan configuration:
     - Added `com.bluecone.app.infra.wechat.openplatform` to scan path
     - Added `annotationClass = org.apache.ibatis.annotations.Mapper.class` to filter only `@Mapper` annotated interfaces
     - This ensures `WechatComponentCredentialMapper` and `WechatAuthorizedAppMapper` are detected while excluding non-mapper interfaces like `WeChatOpenPlatformClient`

## Prevention

To prevent similar issues in the future:

1. Always use `CREATE TABLE IF NOT EXISTS` for new table migrations
2. Test migrations in a clean database before committing
3. Use Flyway's `validate-on-migrate: false` in development (already configured in `application-local.yml`)
4. Consider using Flyway's baseline feature for existing databases

