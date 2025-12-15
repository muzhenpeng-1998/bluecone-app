package com.bluecone.app.platform.codegen.publicid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * PublicIdLookup 代码生成入口。
 *
 * <p>运行方式（generate-sources 阶段由 exec-maven-plugin 调用）：</p>
 * <pre>
 *   java com.bluecone.app.platform.codegen.publicid.PublicIdLookupCodegenMain \\
 *       app-infra/src/main/resources/public-id-resources.yaml \\
 *       app-infra/target/generated-sources/publicid \\
 *       200
 * </pre>
 */
public final class PublicIdLookupCodegenMain {

    private PublicIdLookupCodegenMain() {}

    public static void main(String[] args) throws Exception {
        Path yamlPath = args.length >= 1
                ? Paths.get(args[0])
                : Paths.get("src/main/resources/public-id-resources.yaml");
        Path outputDir = args.length >= 2
                ? Paths.get(args[1])
                : Paths.get("target/generated-sources/publicid");
        int maxBatchSize = args.length >= 3 && StringUtils.isNumeric(args[2])
                ? Integer.parseInt(args[2])
                : 200;

        if (!Files.exists(yamlPath)) {
            System.out.printf("[publicid-codegen] YAML 未找到，跳过生成：%s%n", yamlPath.toAbsolutePath());
            return;
        }

        if (Files.size(yamlPath) == 0) {
            System.out.printf("[publicid-codegen] YAML 为空，跳过生成：%s%n", yamlPath.toAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PublicIdResourcesConfig config = mapper.readValue(yamlPath.toFile(), PublicIdResourcesConfig.class);
        List<PublicIdResourceDefinition> resources = config.getResources();
        if (resources.isEmpty()) {
            System.out.printf("[publicid-codegen] YAML 未配置资源，跳过生成：%s%n", yamlPath.toAbsolutePath());
            return;
        }

        PublicIdLookupCodeGenerator generator = new PublicIdLookupCodeGenerator(maxBatchSize);
        generator.generate(resources, outputDir)
                .forEach(PublicIdLookupCodeGenerator.GeneratedFile::write);

        System.out.printf("[publicid-codegen] 生成完成，共 %d 个资源，输出目录：%s%n",
                resources.size(), outputDir.toAbsolutePath());
    }
}

