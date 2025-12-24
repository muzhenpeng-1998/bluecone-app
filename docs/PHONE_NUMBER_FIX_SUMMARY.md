# Phone Number Retrieval Fix Summary

## Issue

The WeChat mini app login flow was incorrectly handling phone number retrieval when using the new `phoneCode` parameter.

### Problem Details

**File**: `app-member/src/main/java/com/bluecone/app/member/application/auth/UserAuthApplicationService.java`

**Line 84-85** (before fix):
```java
WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
        appId, cmd.getPhoneCode(), null, null);
```

**Issues**:
1. ❌ Called `decryptPhoneNumber()` instead of `getPhoneNumberByCode()`
2. ❌ Passed `phoneCode` as the second parameter (which expects `sessionKey`)
3. ❌ Passed `null` for `encryptedData` and `iv`, which would always fail
4. ❌ The `getPhoneNumberByCode()` method existed but was never used

## Solution

### Fixed Logic Priority

```java
if (phoneCode exists) {
    → Call weChatMiniAppClient.getPhoneNumberByCode(appId, phoneCode)
} else if (encryptedData && iv exist) {
    → Call weChatMiniAppClient.decryptPhoneNumber(appId, sessionKey, encryptedData, iv)
} else {
    → phone remains null (allowed)
}
```

### Implementation Changes

1. **Correct Method Call**:
   - Changed from `decryptPhoneNumber(appId, phoneCode, null, null)`
   - To `getPhoneNumberByCode(appId, phoneCode)`

2. **Added Exception Handling**:
   - Wrapped both phone retrieval paths in try-catch blocks
   - Prevents phone retrieval failures from breaking the entire login flow

3. **Improved Logging**:
   - Changed from `log.debug()` to `log.info()` for success cases
   - Added `log.warn()` for null results
   - Added `log.error()` for exceptions
   - **Security**: Logs only show success/failure, NOT phone number plaintext

4. **Added Missing Case**:
   - Added `log.debug()` when neither phoneCode nor encryptedData/iv is provided
   - Makes it clear that phone retrieval was intentionally skipped

## Code Changes

### Before
```java
// 3. 解密手机号（可选）
String phone = null;
String countryCode = "+86";
if (StringUtils.hasText(cmd.getPhoneCode())) {
    // 优先使用 phoneCode 方式（推荐）
    WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
            appId, cmd.getPhoneCode(), null, null);  // ❌ WRONG!
    if (phoneResult != null) {
        phone = phoneResult.getPhoneNumber();
        countryCode = phoneResult.getCountryCode();
        log.debug("[UserAuth] 获取手机号成功（phoneCode）");
    }
} else if (StringUtils.hasText(cmd.getEncryptedData()) && StringUtils.hasText(cmd.getIv())) {
    // 兼容旧版本 encryptedData/iv 方式
    WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
            appId, sessionResult.getSessionKey(), cmd.getEncryptedData(), cmd.getIv());
    if (phoneResult != null) {
        phone = phoneResult.getPhoneNumber();
        countryCode = phoneResult.getCountryCode();
        log.debug("[UserAuth] 获取手机号成功（encryptedData/iv）");
    }
}
```

### After
```java
// 3. 获取手机号（可选）
String phone = null;
String countryCode = "+86";
if (StringUtils.hasText(cmd.getPhoneCode())) {
    // 优先使用 phoneCode 方式（推荐，新版本）
    try {
        WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.getPhoneNumberByCode(
                appId, cmd.getPhoneCode());  // ✅ CORRECT!
        if (phoneResult != null) {
            phone = phoneResult.getPhoneNumber();
            countryCode = phoneResult.getCountryCode();
            log.info("[UserAuth] 获取手机号成功（phoneCode方式）");  // ✅ No plaintext
        } else {
            log.warn("[UserAuth] 获取手机号失败（phoneCode方式返回null）");
        }
    } catch (Exception e) {
        log.error("[UserAuth] 获取手机号失败（phoneCode方式）: {}", e.getMessage());
    }
} else if (StringUtils.hasText(cmd.getEncryptedData()) && StringUtils.hasText(cmd.getIv())) {
    // 兼容旧版本 encryptedData/iv 方式
    try {
        WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
                appId, sessionResult.getSessionKey(), cmd.getEncryptedData(), cmd.getIv());
        if (phoneResult != null) {
            phone = phoneResult.getPhoneNumber();
            countryCode = phoneResult.getCountryCode();
            log.info("[UserAuth] 获取手机号成功（encryptedData/iv方式）");  // ✅ No plaintext
        } else {
            log.warn("[UserAuth] 获取手机号失败（encryptedData/iv方式返回null）");
        }
    } catch (Exception e) {
        log.error("[UserAuth] 获取手机号失败（encryptedData/iv方式）: {}", e.getMessage());
    }
} else {
    log.debug("[UserAuth] 未提供手机号相关参数（phoneCode或encryptedData/iv），跳过手机号获取");
}
```

## Testing

### Compilation Test
```bash
✅ mvn clean compile -pl app-member
```

### Unit Tests
```bash
✅ mvn -q -DskipTests=false test -pl app-member
```

All tests pass successfully.

## Behavior Changes

### Scenario 1: New Version (phoneCode provided)
- **Before**: ❌ Called wrong method, always failed
- **After**: ✅ Calls `getPhoneNumberByCode()`, works correctly

### Scenario 2: Old Version (encryptedData + iv provided)
- **Before**: ✅ Worked correctly
- **After**: ✅ Still works correctly (backward compatible)

### Scenario 3: No phone parameters
- **Before**: ⚠️ Silent skip
- **After**: ✅ Logs debug message, clear intent

### Scenario 4: Phone retrieval fails
- **Before**: ❌ Could crash login flow
- **After**: ✅ Catches exception, allows login to continue without phone

## Security Improvements

1. **No Phone Number in Logs**: 
   - Only logs "成功" or "失败"
   - Never logs actual phone number plaintext

2. **Graceful Degradation**:
   - Phone retrieval failure doesn't break login
   - User can still login, just without phone number

## Files Modified

1. `app-member/src/main/java/com/bluecone/app/member/application/auth/UserAuthApplicationService.java`
   - Fixed phone number retrieval logic
   - Improved error handling
   - Enhanced logging

## Related Documentation

- WeChat Open Platform Config: `docs/runbook/wechat-open-platform-config.md`
- WeChat Implementation: `WECHAT_IMPLEMENTATION_FINAL_SUMMARY.md`

## Conclusion

✅ **Fixed**: Phone number retrieval now uses the correct method (`getPhoneNumberByCode`)
✅ **Backward Compatible**: Old `encryptedData/iv` method still works
✅ **Secure**: No phone numbers in logs
✅ **Robust**: Exception handling prevents login failures
✅ **Tests Passing**: All unit tests pass

**Status**: ✅ Complete and Tested

