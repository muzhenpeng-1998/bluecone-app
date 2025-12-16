package com.bluecone.app.id.publicid.api;

/**
 * 对外公开 ID 表示，包含业务类型前缀和完整字符串值。
 *
 * @param type  业务类型/前缀（如 ord、sto、ten、usr）
 * @param value 完整的公开 ID 字符串
 */
public record PublicId(String type, String value) {

    /**
     * 以字符串形式返回完整的公开 ID。
     *
     * @return 公开 ID 字符串
     */
    public String asString() {
        return value;
    }
}
