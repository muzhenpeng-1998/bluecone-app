package com.bluecone.app.product.runtime.model;

import java.io.Serializable;

/**
 * 门店菜单快照数据（Prompt 08）。
 * <p>
 * 用于 {@link com.bluecone.app.product.runtime.application.StoreMenuSnapshotProvider} 的缓存数据结构。
 * <p>
 * 包含：
 * <ul>
 *   <li>menuJson：完整的菜单快照 JSON 字符串</li>
 *   <li>version：快照版本号，用于版本校验</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
public record StoreMenuSnapshotData(
        String menuJson,
        Long version
) implements Serializable {
}

