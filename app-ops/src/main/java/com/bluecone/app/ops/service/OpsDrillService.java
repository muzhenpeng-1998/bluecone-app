package com.bluecone.app.ops.service;

import com.bluecone.app.ops.api.dto.drill.ConsumeItem;
import com.bluecone.app.ops.api.dto.drill.IdemConflictItem;
import com.bluecone.app.ops.api.dto.drill.OutboxItem;
import com.bluecone.app.ops.api.dto.drill.PageResult;

public interface OpsDrillService {

    PageResult<OutboxItem> listOutbox(String status, Long beforeId, int limit);

    PageResult<ConsumeItem> listConsume(String consumerGroup, String status, Long beforeId, int limit);

    PageResult<IdemConflictItem> listIdemConflicts(Long beforeId, int limit);
}

