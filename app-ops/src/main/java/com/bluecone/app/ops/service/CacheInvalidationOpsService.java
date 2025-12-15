package com.bluecone.app.ops.service;

import com.bluecone.app.ops.api.dto.cacheinval.CacheInvalItem;
import com.bluecone.app.ops.api.dto.cacheinval.CacheInvalSummary;
import com.bluecone.app.ops.api.dto.drill.PageResult;

public interface CacheInvalidationOpsService {

    CacheInvalSummary getSummary(String window);

    PageResult<CacheInvalItem> listRecent(String window,
                                          String cursor,
                                          int limit,
                                          Long tenantId,
                                          String scope,
                                          String namespace);
}

