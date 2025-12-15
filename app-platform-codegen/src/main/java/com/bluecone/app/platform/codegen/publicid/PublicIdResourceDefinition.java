package com.bluecone.app.platform.codegen.publicid;

/**
 * YAML 中的一条资源定义。
 */
public class PublicIdResourceDefinition {
    private String type;
    private String table;
    private String pkColumn;
    private String tenantColumn;
    private String publicIdColumn;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getPkColumn() {
        return pkColumn;
    }

    public void setPkColumn(String pkColumn) {
        this.pkColumn = pkColumn;
    }

    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }

    public String getPublicIdColumn() {
        return publicIdColumn;
    }

    public void setPublicIdColumn(String publicIdColumn) {
        this.publicIdColumn = publicIdColumn;
    }
}

