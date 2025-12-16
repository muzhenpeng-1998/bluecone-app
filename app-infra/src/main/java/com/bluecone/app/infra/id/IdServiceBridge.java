package com.bluecone.app.infra.id;

import org.springframework.stereotype.Service;

import com.bluecone.app.core.id.IdService;
import com.bluecone.app.core.id.IdType;
import com.bluecone.app.core.id.TypedId;

/**
 * 桥接适配器：将新的 com.bluecone.app.id.api.IdService 适配到旧的 com.bluecone.app.core.id.IdService。
 * 
 * <p>职责：为业务代码提供向后兼容的 IdService Bean，内部委托给新的 ID 模块实现。
 */
@Service
public class IdServiceBridge implements IdService {

    private final com.bluecone.app.id.api.IdService delegate;

    public IdServiceBridge(com.bluecone.app.id.api.IdService delegate) {
        this.delegate = delegate;
    }

    @Override
    public String nextId() {
        return delegate.nextUlidString();
    }

    @Override
    public String nextId(IdType type) {
        String ulid = delegate.nextUlidString();
        return type.apply(ulid);
    }

    @Override
    public TypedId nextTypedId(IdType type) {
        return TypedId.of(type, nextId(type));
    }
}

