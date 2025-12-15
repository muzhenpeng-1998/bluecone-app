package com.bluecone.app.platform.codegen.publicid;

import com.bluecone.app.id.api.ResourceType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 根据 YAML 定义生成 PublicIdLookup Java 源码。
 */
public class PublicIdLookupCodeGenerator {

    private final int maxBatchSize;

    public PublicIdLookupCodeGenerator(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public List<GeneratedFile> generate(List<PublicIdResourceDefinition> resources, Path outputDir) {
        List<GeneratedFile> files = new ArrayList<>();
        if (resources == null || resources.isEmpty()) {
            return files;
        }
        for (PublicIdResourceDefinition resource : resources) {
            validate(resource);
            files.add(generateLookup(resource, outputDir));
        }
        files.add(generateConfiguration(resources, outputDir));
        return files;
    }

    private void validate(PublicIdResourceDefinition resource) {
        Objects.requireNonNull(resource.getType(), "type is required");
        Objects.requireNonNull(resource.getTable(), "table is required");
        Objects.requireNonNull(resource.getPkColumn(), "pkColumn is required");
        Objects.requireNonNull(resource.getTenantColumn(), "tenantColumn is required");
        Objects.requireNonNull(resource.getPublicIdColumn(), "publicIdColumn is required");
        // 校验 type 枚举合法性，避免拼写漂移
        ResourceType.valueOf(resource.getType());
    }

    private GeneratedFile generateLookup(PublicIdResourceDefinition resource, Path outputDir) {
        String typeName = toPascal(resource.getType());
        String className = typeName + "PublicIdLookup";
        String packageName = "com.bluecone.app.infra.publicid.lookup";
        String sqlFindOne = String.format(
                "SELECT %s FROM %s WHERE %s=:tenantId AND %s=:publicId LIMIT 1",
                resource.getPkColumn(), resource.getTable(), resource.getTenantColumn(), resource.getPublicIdColumn());
        String sqlFindMany = String.format(
                "SELECT %s AS pid, %s AS pk FROM %s WHERE %s=:tenantId AND %s IN (:publicIds)",
                resource.getPublicIdColumn(), resource.getPkColumn(), resource.getTable(),
                resource.getTenantColumn(), resource.getPublicIdColumn());

        String content = """
                package %s;

                import com.bluecone.app.core.publicid.api.PublicIdLookup;
                import com.bluecone.app.id.api.ResourceType;
                import java.util.Collections;
                import java.util.LinkedHashMap;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;
                import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
                import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

                /**
                 * 自动生成的 PublicIdLookup：资源类型 %s。
                 * 仅查询必要列，依赖 (%s, %s) 联合索引。
                 */
                public class %s implements PublicIdLookup {

                    private static final int MAX_BATCH_SIZE = %d;
                    private static final String SQL_FIND_ONE = "%s";
                    private static final String SQL_FIND_MANY = "%s";

                    private final NamedParameterJdbcTemplate jdbcTemplate;

                    public %s(NamedParameterJdbcTemplate jdbcTemplate) {
                        this.jdbcTemplate = jdbcTemplate;
                    }

                    @Override
                    public ResourceType type() {
                        return ResourceType.%s;
                    }

                    @Override
                    public Optional<Object> findInternalId(long tenantId, String publicId) {
                        if (publicId == null) {
                            return Optional.empty();
                        }
                        MapSqlParameterSource params = new MapSqlParameterSource()
                                .addValue("tenantId", tenantId)
                                .addValue("publicId", publicId);
                        List<Object> result = jdbcTemplate.query(SQL_FIND_ONE, params, (rs, rowNum) -> rs.getObject(1));
                        if (result.isEmpty()) {
                            return Optional.empty();
                        }
                        return Optional.ofNullable(result.getFirst());
                    }

                    @Override
                    public Map<String, Object> findInternalIds(long tenantId, List<String> publicIds) {
                        if (publicIds == null || publicIds.isEmpty()) {
                            return Collections.emptyMap();
                        }
                        Map<String, Object> result = new LinkedHashMap<>();
                        for (List<String> batch : partition(publicIds, MAX_BATCH_SIZE)) {
                            MapSqlParameterSource params = new MapSqlParameterSource()
                                    .addValue("tenantId", tenantId)
                                    .addValue("publicIds", batch);
                            jdbcTemplate.query(SQL_FIND_MANY, params, rs -> {
                                result.put(rs.getString("pid"), rs.getObject("pk"));
                            });
                        }
                        return result;
                    }

                    private List<List<String>> partition(List<String> input, int size) {
                        int total = input.size();
                        if (total <= size) {
                            return List.of(input);
                        }
                        int batches = (total + size - 1) / size;
                        List<List<String>> parts = new java.util.ArrayList<>(batches);
                        for (int i = 0; i < total; i += size) {
                            parts.add(input.subList(i, Math.min(total, i + size)));
                        }
                        return parts;
                    }
                }
                """.formatted(
                packageName,
                resource.getType(),
                resource.getTenantColumn(), resource.getPublicIdColumn(),
                className,
                maxBatchSize,
                sqlFindOne,
                sqlFindMany,
                className,
                resource.getType()
        );

        Path packageDir = outputDir.resolve("com/bluecone/app/infra/publicid/lookup");
        Path file = packageDir.resolve(className + ".java");
        return new GeneratedFile(file, content);
    }

    private GeneratedFile generateConfiguration(List<PublicIdResourceDefinition> resources, Path outputDir) {
        String packageName = "com.bluecone.app.infra.publicid.lookup";
        Path packageDir = outputDir.resolve("com/bluecone/app/infra/publicid/lookup");
        Path file = packageDir.resolve("PublicIdLookupGeneratedConfiguration.java");

        StringJoiner beans = new StringJoiner("\n\n");
        for (PublicIdResourceDefinition resource : resources) {
            String typeName = toPascal(resource.getType());
            String className = typeName + "PublicIdLookup";
            String beanName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "PublicIdLookup";
            beans.add("""
                    @Bean
                    @ConditionalOnMissingBean(name = "%s")
                    public %s %s(NamedParameterJdbcTemplate jdbcTemplate) {
                        return new %s(jdbcTemplate);
                    }""".formatted(beanName, className, beanName, className));
        }

        String content = """
                package %s;

                import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

                /**
                 * 自动生成的 PublicIdLookup 聚合配置。
                 */
                @Configuration
                public class PublicIdLookupGeneratedConfiguration {

                %s
                }
                """.formatted(packageName, indent(beans.toString(), 4));

        return new GeneratedFile(file, content);
    }

    private String toPascal(String type) {
        String lower = type.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String indent(String text, int spaces) {
        String prefix = " ".repeat(spaces);
        return text.lines().map(line -> prefix + line).reduce((a, b) -> a + "\n" + b).orElse("");
    }

    /**
     * 生成文件封装。
     */
    public record GeneratedFile(Path path, String content) {
        public void write() {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, header() + content);
            } catch (Exception ex) {
                throw new RuntimeException("写入生成文件失败: " + path, ex);
            }
        }

        private String header() {
            return """
                    // Code generated by PublicIdLookupCodegen at %s
                    // DO NOT EDIT MANUALLY

                    """.formatted(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
}

