package com.bluecone.app.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ID 现状盘点工具。
 *
 * <p>扫描各模块 src/main/java 下的实体/DO/DTO 类，统计名称类似 id/internalId/publicId/xxxId 的字段，
 * 并生成 docs/engineering/id-inventory.md 报告。</p>
 *
 * <p>使用方式（在仓库根目录运行）：</p>
 *
 * <pre>
 *   mvn -pl app-application -DskipTests compile
 *   java -cp app-application/target/classes com.bluecone.app.tools.IdInventoryTool .
 * </pre>
 */
public class IdInventoryTool {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*(public|protected|private)\\s+([A-Za-z0-9_<>.,?\\[\\]]+)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*[;=].*");

    private record FieldInfo(
            String module,
            String className,
            String filePath,
            String fieldName,
            String fieldType
    ) {
    }

    public static void main(String[] args) throws IOException {
        Path root = args.length > 0
                ? Paths.get(args[0]).toAbsolutePath().normalize()
                : Paths.get(".").toAbsolutePath().normalize();

        List<FieldInfo> fields = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java") && file.toString().contains("src/main/java")) {
                    if (isCandidatePojo(file)) {
                        scanFile(root, file, fields);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        fields.sort(Comparator
                .comparing(FieldInfo::module)
                .thenComparing(FieldInfo::className)
                .thenComparing(FieldInfo::fieldName));

        Path out = root.resolve("docs/engineering/id-inventory.md");
        Files.createDirectories(out.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writeReport(writer, fields);
        }
    }

    private static boolean isCandidatePojo(Path file) {
        String path = file.toString();
        String name = file.getFileName().toString();
        boolean nameMatch = name.contains("Entity")
                || name.contains("DTO")
                || name.contains("Dto")
                || name.contains("DO")
                || name.contains("Po")
                || name.contains("PO");
        boolean pkgMatch = path.contains("/dataobject/") || path.contains("\\dataobject\\");
        return nameMatch || pkgMatch;
    }

    private static void scanFile(Path root, Path file, List<FieldInfo> out) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String pkg = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ")) {
                int semi = line.indexOf(';');
                if (semi > 0) {
                    pkg = line.substring("package ".length(), semi).trim();
                }
                break;
            }
        }
        String simpleName = stripSuffix(file.getFileName().toString(), ".java");
        String className = (pkg != null && !pkg.isEmpty()) ? pkg + "." + simpleName : simpleName;

        String module = null;
        Path rel = root.relativize(file);
        if (rel.getNameCount() > 0) {
            module = rel.getName(0).toString();
        }
        if (module == null) {
            module = "";
        }

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("//") || line.startsWith("*") || line.startsWith("@")) {
                continue;
            }
            if (line.contains(" class ") || line.contains(" interface ") || line.contains(" enum ")) {
                continue;
            }
            Matcher m = FIELD_PATTERN.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String type = m.group(2).trim();
            String name = m.group(3).trim();
            if (!isIdLikeField(name)) {
                continue;
            }
            out.add(new FieldInfo(
                    module,
                    className,
                    root.relativize(file).toString(),
                    name,
                    type
            ));
        }
    }

    private static boolean isIdLikeField(String name) {
        String n = Objects.requireNonNull(name).toLowerCase();
        if (n.equals("id") || n.equals("internalid") || n.equals("publicid")) {
            return true;
        }
        return n.endsWith("id");
    }

    private static String stripSuffix(String value, String suffix) {
        if (value != null && value.endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        }
        return value;
    }

    private static void writeReport(BufferedWriter writer, List<FieldInfo> fields) throws IOException {
        writer.write("# ID Inventory Report");
        writer.newLine();
        writer.newLine();
        writer.write("> 本文件由 IdInventoryTool 自动生成，用于盘点各模块实体/DTO 中的 ID 字段类型。");
        writer.newLine();
        writer.newLine();

        writer.write("## 全量字段清单");
        writer.newLine();
        writer.newLine();
        writer.write("| Module | Class | Field | Java Type | File |");
        writer.newLine();
        writer.write("|--------|-------|-------|-----------|------|");
        writer.newLine();

        for (FieldInfo f : fields) {
            writer.write("| ");
            writer.write(escape(f.module()));
            writer.write(" | ");
            writer.write(escape(f.className()));
            writer.write(" | ");
            writer.write(escape(f.fieldName()));
            writer.write(" | ");
            writer.write(escape(f.fieldType()));
            writer.write(" | ");
            writer.write(escape(f.filePath()));
            writer.write(" |");
            writer.newLine();
        }

        writer.newLine();
        writer.newLine();

        Map<Category, List<FieldInfo>> byCategory = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            byCategory.put(c, new ArrayList<>());
        }
        for (FieldInfo f : fields) {
            Category c = classify(f);
            byCategory.get(c).add(f);
        }

        // 建议 1：应迁移为 Ulid128 的字段
        writer.write("## 建议一：应迁移为 Ulid128（BINARY(16)）的字段");
        writer.newLine();
        writer.newLine();
        writer.write("> 规则：类型为 Long/String，字段名以 Id 结尾或为 id/internalId，且当前未使用 Ulid128。");
        writer.newLine();
        writer.newLine();
        writer.write("| Module | Class | Field | Java Type | File | 建议理由 |");
        writer.newLine();
        writer.write("|--------|-------|-------|-----------|------|----------|");
        writer.newLine();
        for (FieldInfo f : byCategory.get(Category.ULID_CANDIDATE)) {
            writer.write("| ");
            writer.write(escape(f.module()));
            writer.write(" | ");
            writer.write(escape(f.className()));
            writer.write(" | ");
            writer.write(escape(f.fieldName()));
            writer.write(" | ");
            writer.write(escape(f.fieldType()));
            writer.write(" | ");
            writer.write(escape(f.filePath()));
            writer.write(" | ");
            writer.write("内部标识当前为 Long/String，建议统一迁移为 Ulid128 以配合 BINARY(16) 主键与 PublicId 体系");
            writer.write(" |");
            writer.newLine();
        }

        writer.newLine();
        writer.write("## 建议二：应作为 public_id 使用的字段");
        writer.newLine();
        writer.newLine();
        writer.write("> 规则：字段名包含 publicId 且类型为 String。");
        writer.newLine();
        writer.newLine();
        writer.write("| Module | Class | Field | Java Type | File | 建议理由 |");
        writer.newLine();
        writer.write("|--------|-------|-------|-----------|------|----------|");
        writer.newLine();
        for (FieldInfo f : byCategory.get(Category.PUBLIC_ID)) {
            writer.write("| ");
            writer.write(escape(f.module()));
            writer.write(" | ");
            writer.write(escape(f.className()));
            writer.write(" | ");
            writer.write(escape(f.fieldName()));
            writer.write(" | ");
            writer.write(escape(f.fieldType()));
            writer.write(" | ");
            writer.write(escape(f.filePath()));
            writer.write(" | ");
            writer.write("该字段语义上为对外标识，建议标准化为 PublicId（prefix_ulid）并建立唯一索引");
            writer.write(" |");
            writer.newLine();
        }

        writer.newLine();
        writer.write("## 建议三：应保留为 long/BIGINT 的字段");
        writer.newLine();
        writer.newLine();
        writer.write("> 规则：实体/DO 类中的 Long 类型主键 id，通常对应既有 BIGINT 自增主键或 Snowflake longId。");
        writer.newLine();
        writer.newLine();
        writer.write("| Module | Class | Field | Java Type | File | 保留理由 |");
        writer.newLine();
        writer.write("|--------|-------|-------|-----------|------|----------|");
        writer.newLine();
        for (FieldInfo f : byCategory.get(Category.MUST_LONG)) {
            writer.write("| ");
            writer.write(escape(f.module()));
            writer.write(" | ");
            writer.write(escape(f.className()));
            writer.write(" | ");
            writer.write(escape(f.fieldName()));
            writer.write(" | ");
            writer.write(escape(f.fieldType()));
            writer.write(" | ");
            writer.write(escape(f.filePath()));
            writer.write(" | ");
            writer.write("对应现有 BIGINT 主键或与第三方集成强绑定，短期内保留 long 以避免大规模数据迁移");
            writer.write(" |");
            writer.newLine();
        }

        writer.newLine();
        writer.write("## 说明");
        writer.newLine();
        writer.newLine();
        writer.write("- 本表仅基于简单规则识别：类名包含 Entity/DTO/DO/PO，或路径包含 dataobject。");
        writer.newLine();
        writer.write("- 字段名规则：名称等于 id/internalId/publicId，或以 Id/ID 结尾。");
        writer.newLine();
        writer.write("- 上述三类建议基于简单启发式规则生成，具体迁移方案仍需结合业务场景人工确认。");
        writer.newLine();
    }

    private static String escape(String v) {
        return v.replace("|", "\\|");
    }

    private static Category classify(FieldInfo f) {
        String typeName = baseType(f.fieldType()).toLowerCase();
        String n = f.fieldName().toLowerCase();
        String cls = f.className().toLowerCase();

        boolean isString = typeName.equals("string") || typeName.equals("java.lang.string");
        boolean isLong = typeName.equals("long") || typeName.equals("java.lang.long");
        boolean isUlid = typeName.equals("ulid128");

        if (n.contains("publicid") && isString) {
            return Category.PUBLIC_ID;
        }

        if (isUlid) {
            return Category.NONE;
        }

        if (isLong && n.equals("id") && (cls.contains("entity") || cls.contains("do"))) {
            return Category.MUST_LONG;
        }

        if ((isLong || isString) && (n.endsWith("id") || n.equals("id") || n.equals("internalid"))) {
            return Category.ULID_CANDIDATE;
        }

        return Category.NONE;
    }

    private static String baseType(String rawType) {
        if (rawType == null) {
            return "";
        }
        String t = rawType.trim();
        int lt = t.indexOf('<');
        if (lt > 0) {
            t = t.substring(0, lt);
        }
        int space = t.indexOf(' ');
        if (space > 0) {
            t = t.substring(0, space);
        }
        int arr = t.indexOf('[');
        if (arr > 0) {
            t = t.substring(0, arr);
        }
        int dot = t.lastIndexOf('.');
        if (dot >= 0 && dot < t.length() - 1) {
            t = t.substring(dot + 1);
        }
        return t;
    }
}
    enum Category {
        ULID_CANDIDATE,
        PUBLIC_ID,
        MUST_LONG,
        NONE
    }
