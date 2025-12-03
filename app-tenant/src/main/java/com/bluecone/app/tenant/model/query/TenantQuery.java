package com.bluecone.app.tenant.model.query;

/**
 * 租户查询条件。
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
