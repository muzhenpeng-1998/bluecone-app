package com.bluecone.app.ops.api.dto.drill;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        String nextCursor,
        int limit
) {
}

