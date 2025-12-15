package com.bluecone.app.id.segment;

/**
 * ID 号段范围，表示一个连续的 ID 区间 [startInclusive, endInclusive]。
 * 
 * <p>号段由数据库分配，应用本地缓存并按序消费。
 * 
 * @param startInclusive 起始 ID（包含）
 * @param endInclusive 结束 ID（包含）
 */
public record SegmentRange(long startInclusive, long endInclusive) {
    
    /**
     * 构造函数，校验号段范围合法性。
     * 
     * @param startInclusive 起始 ID（包含）
     * @param endInclusive 结束 ID（包含）
     * @throws IllegalArgumentException 如果 startInclusive > endInclusive
     */
    public SegmentRange {
        if (startInclusive > endInclusive) {
            throw new IllegalArgumentException(
                String.format("非法号段范围: start=%d > end=%d", startInclusive, endInclusive)
            );
        }
    }
    
    /**
     * 返回号段包含的 ID 数量。
     * 
     * @return ID 数量
     */
    public long size() {
        return endInclusive - startInclusive + 1;
    }
    
    /**
     * 判断指定 ID 是否在当前号段范围内。
     * 
     * @param id 待判断的 ID
     * @return 如果 ID 在范围内返回 true，否则返回 false
     */
    public boolean contains(long id) {
        return id >= startInclusive && id <= endInclusive;
    }
}

