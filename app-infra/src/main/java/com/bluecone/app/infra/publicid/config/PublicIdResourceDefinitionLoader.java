package com.bluecone.app.infra.publicid.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * 从 classpath 读取 public-id-resources.yaml。
 */
public class PublicIdResourceDefinitionLoader {
    private static final Logger log = LoggerFactory.getLogger(PublicIdResourceDefinitionLoader.class);
    private static final String RESOURCE_PATH = "public-id-resources.yaml";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public List<PublicIdResourceDefinition> load() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        if (!resource.exists()) {
            log.info("public-id-resources.yaml 未找到，跳过公共 ID 资源加载");
            return Collections.emptyList();
        }
        try (InputStream in = resource.getInputStream()) {
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (yaml.isBlank()) {
                log.info("public-id-resources.yaml 为空，跳过公共 ID 资源加载");
                return Collections.emptyList();
            }
            PublicIdResourcesConfig config = mapper.readValue(yaml, PublicIdResourcesConfig.class);
            return config.getResources();
        } catch (IOException ex) {
            throw new IllegalStateException("读取 public-id-resources.yaml 失败", ex);
        }
    }
}

