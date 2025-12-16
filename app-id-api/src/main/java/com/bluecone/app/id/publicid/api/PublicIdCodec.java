package com.bluecone.app.id.publicid.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * PublicId 编解码接口，用于在内部 ULID 与对外公开 ID 之间进行转换。
 */
public interface PublicIdCodec {

    /**
     * 将内部 ULID 编码为公开 ID。
     *
     * @param type 业务类型前缀（如 ord、sto、ten、usr）
     * @param id   内部 ULID 128 位表示
     * @return 公开 ID
     */
    PublicId encode(String type, Ulid128 id);

    /**
     * 将内部 ULID 的 16 字节表示编码为公开 ID。
     *
     * @param type        业务类型前缀
     * @param ulidBytes16 16 字节 ULID 二进制表示
     * @return 公开 ID
     */
    PublicId encode(String type, byte[] ulidBytes16);

    /**
     * 将公开 ID 字符串解析为内部表示。
     *
     * @param publicId 公开 ID 字符串
     * @return 解析结果，包含业务类型与内部 ULID
     * @throws IllegalArgumentException 当格式非法、校验失败或类型不合法时
     */
    DecodedPublicId decode(String publicId);

    /**
     * 校验公开 ID 是否格式合法且校验和正确。
     *
     * @param publicId 公开 ID 字符串
     * @return true 表示合法，false 表示非法
     */
    boolean isValid(String publicId);
}
