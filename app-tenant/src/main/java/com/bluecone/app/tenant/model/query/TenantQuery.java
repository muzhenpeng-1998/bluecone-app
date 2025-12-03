package com.bluecone.app.tenant.model.query;

/**
 * 租户查询条件。
 * - keyword 支持名称/编码模糊查询
 * - status 过滤启用/禁用
 * - planId 预留按套餐过滤
 * - pageNo/pageSize 做安全兜底（pageSize 最大 200）
 */
public record TenantQuery(
        String keyword,
        Integer status,
        Long planId,
        int pageNo,
        int pageSize) {

    public TenantQuery {
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 200) {
            pageSize = 200;
        }
    }
}
