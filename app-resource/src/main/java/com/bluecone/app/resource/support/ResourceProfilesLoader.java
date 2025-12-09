package com.bluecone.app.resource.support;

import com.bluecone.app.infra.storage.AccessLevel;
import com.bluecone.app.infra.storage.aliyun.AliyunOssProperties;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;
import com.bluecone.app.resource.api.enums.ResourcePurpose;
import com.bluecone.app.resource.api.exception.ResourceUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 资源 profile 加载器，后续用于读取配置中心或文件。
 */
@Component
public class ResourceProfilesLoader {

    private final Map<ResourceProfileCode, ResourceProfileSpec> profiles;
    private final String environment;

    public ResourceProfilesLoader(AliyunOssProperties aliyunOssProperties,
                                  @Value("${spring.profiles.active:default}") String environment) {
        String bucket = StringUtils.hasText(aliyunOssProperties.getDefaultBucket())
                ? aliyunOssProperties.getDefaultBucket()
                : "bluecone";
        this.environment = StringUtils.hasText(environment) ? environment : "default";

        Map<ResourceProfileCode, ResourceProfileSpec> map = new EnumMap<>(ResourceProfileCode.class);
        map.put(ResourceProfileCode.STORE_LOGO, buildSpec(ResourceProfileCode.STORE_LOGO,
                ResourcePurpose.MAIN_LOGO,
                bucket,
                "store-logo",
                AccessLevel.PUBLIC_READ,
                2 * 1024 * 1024L,
                normalize("jpg", "jpeg", "png", "webp"),
                normalize("image/jpeg", "image/png", "image/webp"),
                900));
        map.put(ResourceProfileCode.PRODUCT_IMAGE, buildSpec(ResourceProfileCode.PRODUCT_IMAGE,
                ResourcePurpose.GALLERY,
                bucket,
                "product-images",
                AccessLevel.PUBLIC_READ,
                5 * 1024 * 1024L,
                normalize("jpg", "jpeg", "png", "webp", "gif"),
                normalize("image/jpeg", "image/png", "image/webp", "image/gif"),
                1200));
        map.put(ResourceProfileCode.EXPORT_REPORT, buildSpec(ResourceProfileCode.EXPORT_REPORT,
                ResourcePurpose.AUXILIARY,
                bucket,
                "reports",
                AccessLevel.PRIVATE,
                10 * 1024 * 1024L,
                normalize("xlsx", "xls", "zip", "csv", "pdf"),
                normalize("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel", "application/zip", "text/csv", "application/pdf"),
                600));
        map.put(ResourceProfileCode.SYSTEM_TEMP, buildSpec(ResourceProfileCode.SYSTEM_TEMP,
                ResourcePurpose.AUXILIARY,
                bucket,
                "system-temp",
                AccessLevel.PRIVATE,
                20 * 1024 * 1024L,
                normalize("tmp", "json", "log"),
                normalize("application/json", "text/plain"),
                300));
        map.put(ResourceProfileCode.USER_AVATAR, buildSpec(ResourceProfileCode.USER_AVATAR,
                ResourcePurpose.AVATAR,
                bucket,
                "user-avatar",
                AccessLevel.PUBLIC_READ,
                3 * 1024 * 1024L,
                normalize("jpg", "jpeg", "png", "webp"),
                normalize("image/jpeg", "image/png", "image/webp"),
                600));

        this.profiles = Map.copyOf(map);
    }

    public ResourceProfileSpec getProfile(ResourceProfileCode code) {
        ResourceProfileSpec spec = profiles.get(code);
        if (spec == null) {
            throw new ResourceUploadException("未配置的资源 profile: " + code);
        }
        return spec;
    }

    public String getEnvironment() {
        return environment;
    }

    private static ResourceProfileSpec buildSpec(ResourceProfileCode code,
                                                ResourcePurpose purpose,
                                                String bucket,
                                                String basePath,
                                                AccessLevel accessLevel,
                                                long maxSizeBytes,
                                                Set<String> extensions,
                                                Set<String> contentTypes,
                                                long ttlSeconds) {
        return new ResourceProfileSpec(code,
                purpose,
                bucket,
                basePath,
                accessLevel,
                maxSizeBytes,
                extensions,
                contentTypes,
                ttlSeconds);
    }

    private static Set<String> normalize(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase().trim())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
