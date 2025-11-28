package com.bluecone.app.infra.id;

import org.springframework.stereotype.Service;

import com.bluecone.app.core.id.IdService;
import com.bluecone.app.core.id.IdType;
import com.bluecone.app.core.id.TypedId;
import com.bluecone.app.id.core.UlidIdGenerator;

@Service
public class DefaultIdService implements IdService {

    private final UlidIdGenerator ulidIdGenerator;

    public DefaultIdService(UlidIdGenerator ulidIdGenerator) {
        this.ulidIdGenerator = ulidIdGenerator;
    }

    @Override
    public String nextId() {
        return ulidIdGenerator.nextUlid();
    }

    @Override
    public String nextId(IdType type) {
        String ulid = ulidIdGenerator.nextUlid();
        return type.apply(ulid);
    }

    @Override
    public TypedId nextTypedId(IdType type) {
        return TypedId.of(type, nextId(type));
    }
}
