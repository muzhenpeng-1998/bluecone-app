package com.bluecone.app.id.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 统一 ID 门面接口，提供 ULID / Long ID / PublicId 能力。
 * 
 * <p>BlueCone ID v2 三层体系：
 * <ul>
 *   <li>DB 主键：long 型，高性能，不对外暴露</li>
 *   <li>ULID：128 位，业务内部标识，可选</li>
 *   <li>Public ID：带前缀的对外 ID，格式为 "prefix_ulid"</li>
 * </ul>
 */
public interface IdService {

    /**
     * 生成下一个 ULID，返回强类型 128 位表示。
     *
     * @return 下一个 ULID 对象
     */
    Ulid128 nextUlid();

    /**
     * 生成下一个 ULID 字符串（26 位）。
     *
     * @return ULID 字符串
     */
    String nextUlidString();

    /**
     * 生成下一个 ULID 的二进制表示（16 字节，大端）。
     *
     * @return 16 字节数组，前 8 字节为 MSB，后 8 字节为 LSB
     */
    byte[] nextUlidBytes();

    /**
     * 生成下一个基于号段（Segment）的 long 型 ID。
     * 
     * <p>号段模式特点：
     * <ul>
     *   <li>全局唯一：同一 scope 内单调递增</li>
     *   <li>高性能：本地缓存号段，减少数据库访问</li>
     *   <li>无时钟依赖：避免 Snowflake 的时钟回拨问题</li>
     * </ul>
     *
     * @param scope ID 作用域，对应不同的业务表或领域
     * @return 下一个 long 型 ID
     * @throws IllegalStateException 如果号段分配失败
     */
    long nextLong(IdScope scope);

    /**
     * 生成下一个对外公开 ID（PublicId），格式为 "prefix_ulid"。
     * 
     * <p>示例：
     * <ul>
     *   <li>租户：tnt_01HN8X5K9G3QRST2VW4XYZ</li>
     *   <li>门店：sto_01HN8X5K9G3QRST2VW4XYZ</li>
     *   <li>订单：ord_01HN8X5K9G3QRST2VW4XYZ</li>
     * </ul>
     *
     * @param type 业务资源类型
     * @return 对外公开 ID 字符串
     */
    String nextPublicId(ResourceType type);

    /**
     * 校验 Public ID 的格式和类型是否合法。
     * 
     * <p>校验内容：
     * <ul>
     *   <li>格式：prefix_ulid（下划线分隔）</li>
     *   <li>前缀：是否匹配预期资源类型</li>
     *   <li>ULID：26 位字符合法性</li>
     * </ul>
     *
     * @param expectedType 预期的资源类型
     * @param publicId 待校验的 Public ID
     * @throws IllegalArgumentException 如果格式非法、类型不匹配或 ULID 非法
     */
    void validatePublicId(ResourceType expectedType, String publicId);
}
