package com.bluecone.app.core.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 错误码注册表。
 * <p>在应用启动时自动扫描所有 ErrorCode 实现，检测重复的 code 值。</p>
 * <p>若检测到重复，会打印警告日志并抛出异常，阻止应用启动。</p>
 */
@Component
public class ErrorCodeRegistry implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ErrorCodeRegistry.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("开始检查错误码重复性...");
        
        List<ErrorCode> allErrorCodes = collectAllErrorCodes();
        log.info("共收集到 {} 个错误码", allErrorCodes.size());
        
        checkDuplicateCodes(allErrorCodes);
        
        log.info("错误码重复性检查通过 ✓");
    }

    /**
     * 收集所有错误码。
     * <p>扫描所有实现 ErrorCode 接口的枚举类。</p>
     */
    private List<ErrorCode> collectAllErrorCodes() {
        List<ErrorCode> allCodes = new ArrayList<>();
        
        // 手动注册所有错误码枚举（避免使用反射扫描）
        allCodes.addAll(Arrays.asList(CommonErrorCode.values()));
        allCodes.addAll(Arrays.asList(BizErrorCode.values()));
        allCodes.addAll(Arrays.asList(UserErrorCode.values()));
        allCodes.addAll(Arrays.asList(AuthErrorCode.values()));
        allCodes.addAll(Arrays.asList(ParamErrorCode.values()));
        allCodes.addAll(Arrays.asList(TenantErrorCode.values()));
        allCodes.addAll(Arrays.asList(PublicIdErrorCode.values()));
        
        // TODO: 未来新增错误码类时，需要在此处注册
        // 可选：改为使用 Spring 的 @Autowired List<ErrorCode> 注入方式
        
        return allCodes;
    }

    /**
     * 检查重复的错误码。
     * <p>若发现重复，抛出 IllegalStateException 阻止应用启动。</p>
     */
    private void checkDuplicateCodes(List<ErrorCode> allErrorCodes) {
        Map<String, List<ErrorCode>> codeGroups = allErrorCodes.stream()
                .collect(Collectors.groupingBy(ErrorCode::getCode));
        
        List<String> duplicates = codeGroups.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (!duplicates.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("检测到重复的错误码，请修复后再启动：\n");
            
            for (String code : duplicates) {
                List<ErrorCode> duplicatedCodes = codeGroups.get(code);
                errorMsg.append(String.format("  - code='%s' 重复出现在：\n", code));
                for (ErrorCode ec : duplicatedCodes) {
                    errorMsg.append(String.format("      %s (%s)\n", 
                            ec.getClass().getSimpleName(), 
                            ec.getMessage()));
                }
            }
            
            log.error(errorMsg.toString());
            throw new IllegalStateException("错误码重复冲突，应用启动失败");
        }
    }
}
