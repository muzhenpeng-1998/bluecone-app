package com.bluecone.app.infra.publicid.verify;

import com.bluecone.app.infra.publicid.config.PublicIdResourceDefinition;
import com.bluecone.app.infra.publicid.config.PublicIdResourceDefinitionLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

/**
 * 启动期校验 public_id 索引契约，避免线上慢 SQL。
 */
public class PublicIdSchemaVerifier implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(PublicIdSchemaVerifier.class);

    private final PublicIdVerifyProperties properties;
    private final PublicIdResourceDefinitionLoader loader;
    private final JdbcTemplate jdbcTemplate;

    public PublicIdSchemaVerifier(PublicIdVerifyProperties properties,
                                  PublicIdResourceDefinitionLoader loader,
                                  DataSource dataSource) {
        this.properties = properties;
        this.loader = loader;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            log.info("PublicIdSchemaVerifier 已禁用，跳过校验");
            return;
        }
        List<PublicIdResourceDefinition> definitions = loader.load();
        if (CollectionUtils.isEmpty(definitions)) {
            log.info("未发现 public-id 资源定义，跳过校验");
            return;
        }

        List<String> violations = new ArrayList<>();
        for (PublicIdResourceDefinition def : definitions) {
            validateTable(def, violations);
            validateColumns(def, violations);
            if (properties.isRequireCompositeIndex()) {
                validateIndex(def, violations);
            }
        }

        if (!violations.isEmpty()) {
            String message = "Public ID 索引校验失败：\n" + String.join("\n", violations);
            if (properties.isFailFast()) {
                throw new IllegalStateException(message);
            } else {
                log.warn(message);
            }
        } else {
            log.info("Public ID 索引校验通过，校验资源 {} 个", definitions.size());
        }
    }

    private void validateTable(PublicIdResourceDefinition def, List<String> violations) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = ?",
                Integer.class, def.getTable());
        if (count == null || count == 0) {
            violations.add(error(def, "表不存在，请添加表或更新 YAML 定义"));
        }
    }

    private void validateColumns(PublicIdResourceDefinition def, List<String> violations) {
        Map<String, String> columns = new HashMap<>();
        columns.put("pkColumn", def.getPkColumn());
        columns.put("tenantColumn", def.getTenantColumn());
        columns.put("publicIdColumn", def.getPublicIdColumn());
        columns.forEach((label, column) -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = database() AND table_name = ? AND column_name = ?",
                    Integer.class, def.getTable(), column);
            if (count == null || count == 0) {
                violations.add(error(def, "列缺失：" + column + "（" + label + "）"));
            }
        });
    }

    private void validateIndex(PublicIdResourceDefinition def, List<String> violations) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT index_name, seq_in_index, column_name " +
                        "FROM information_schema.statistics " +
                        "WHERE table_schema = database() AND table_name = ? " +
                        "ORDER BY index_name, seq_in_index",
                def.getTable());
        boolean matched = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r.get("index_name")))
                .values().stream()
                .anyMatch(list -> matchPrefix(list, def));
        if (!matched) {
            String ddl = "CREATE INDEX idx_%s_tenant_public ON %s(%s, %s);"
                    .formatted(def.getTable(), def.getTable(), def.getTenantColumn(), def.getPublicIdColumn());
            violations.add(error(def, "缺少联合索引，推荐 DDL: " + ddl));
        }
    }

    private boolean matchPrefix(List<Map<String, Object>> rows, PublicIdResourceDefinition def) {
        List<String> required = properties.getIndexColumns();
        if (rows.size() < required.size()) {
            return false;
        }
        for (int i = 0; i < required.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String column = (String) row.get("column_name");
            if (!required.get(i).equalsIgnoreCase(column)) {
                return false;
            }
        }
        return true;
    }

    private String error(PublicIdResourceDefinition def, String message) {
        return "[table=%s,type=%s] %s".formatted(def.getTable(), def.getType(), message);
    }
}

