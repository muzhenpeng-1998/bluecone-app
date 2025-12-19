package com.bluecone.app.infra.admin.service;

import com.bluecone.app.infra.admin.entity.AdminAuditLogEntity;
import com.bluecone.app.infra.admin.mapper.AdminAuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志服务
 * 
 * 提供后台操作的审计日志记录功能，支持：
 * - 记录操作前后的数据快照
 * - 自动提取请求上下文信息
 * - 异步写入日志（不阻塞业务）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AdminAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 记录审计日志（异步）
     * 
     * @param builder 审计日志构建器
     */
    @Async
    public void log(AuditLogBuilder builder) {
        try {
            AdminAuditLogEntity entity = builder.build();
            auditLogMapper.insert(entity);
            log.debug("审计日志记录成功: action={}, resourceType={}, resourceId={}", 
                    entity.getAction(), entity.getResourceType(), entity.getResourceId());
        } catch (Exception e) {
            log.error("审计日志记录失败", e);
            // 审计日志失败不应影响业务，仅记录错误日志
        }
    }
    
    /**
     * 创建审计日志构建器
     * 
     * @param tenantId 租户ID
     * @param operatorId 操作人ID
     * @return 审计日志构建器
     */
    public AuditLogBuilder builder(Long tenantId, Long operatorId) {
        return new AuditLogBuilder(tenantId, operatorId, objectMapper);
    }
    
    /**
     * 审计日志构建器
     */
    public static class AuditLogBuilder {
        private final Long tenantId;
        private final Long operatorId;
        private final ObjectMapper objectMapper;
        
        private String operatorName;
        private String action;
        private String resourceType;
        private Long resourceId;
        private String resourceName;
        private String operationDesc;
        private Object dataBefore;
        private Object dataAfter;
        private String status = "SUCCESS";
        private String errorMessage;
        
        public AuditLogBuilder(Long tenantId, Long operatorId, ObjectMapper objectMapper) {
            this.tenantId = tenantId;
            this.operatorId = operatorId;
            this.objectMapper = objectMapper;
        }
        
        public AuditLogBuilder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }
        
        public AuditLogBuilder action(String action) {
            this.action = action;
            return this;
        }
        
        public AuditLogBuilder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }
        
        public AuditLogBuilder resourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }
        
        public AuditLogBuilder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }
        
        public AuditLogBuilder operationDesc(String operationDesc) {
            this.operationDesc = operationDesc;
            return this;
        }
        
        public AuditLogBuilder dataBefore(Object dataBefore) {
            this.dataBefore = dataBefore;
            return this;
        }
        
        public AuditLogBuilder dataAfter(Object dataAfter) {
            this.dataAfter = dataAfter;
            return this;
        }
        
        public AuditLogBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public AuditLogBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            this.status = "FAILED";
            return this;
        }
        
        AdminAuditLogEntity build() {
            AdminAuditLogEntity entity = new AdminAuditLogEntity();
            entity.setTenantId(tenantId);
            entity.setOperatorId(operatorId);
            entity.setOperatorName(operatorName);
            entity.setAction(action);
            entity.setResourceType(resourceType);
            entity.setResourceId(resourceId);
            entity.setResourceName(resourceName);
            entity.setOperationDesc(operationDesc);
            entity.setStatus(status);
            entity.setErrorMessage(errorMessage);
            entity.setCreatedAt(LocalDateTime.now());
            
            // 序列化数据快照
            if (dataBefore != null) {
                entity.setDataBefore(toJson(dataBefore));
            }
            if (dataAfter != null) {
                entity.setDataAfter(toJson(dataAfter));
            }
            
            // 计算变更摘要
            if (dataBefore != null && dataAfter != null) {
                entity.setChangeSummary(calculateChangeSummary(dataBefore, dataAfter));
            }
            
            // 提取请求上下文
            extractRequestContext(entity);
            
            return entity;
        }
        
        private String toJson(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                return obj.toString();
            }
        }
        
        private String calculateChangeSummary(Object before, Object after) {
            // 简单实现：记录变更字段
            // 可以扩展为更复杂的diff算法
            Map<String, String> summary = new HashMap<>();
            summary.put("type", "UPDATED");
            summary.put("hasChanges", "true");
            return toJson(summary);
        }
        
        private void extractRequestContext(AdminAuditLogEntity entity) {
            try {
                ServletRequestAttributes attributes = 
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    entity.setOperatorIp(getClientIp(request));
                    entity.setRequestUri(request.getRequestURI());
                    entity.setRequestMethod(request.getMethod());
                    entity.setUserAgent(request.getHeader("User-Agent"));
                    entity.setTraceId(request.getHeader("X-Trace-Id"));
                }
            } catch (Exception e) {
                // 忽略上下文提取失败
            }
        }
        
        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // 处理多级代理的情况
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
    }
}
