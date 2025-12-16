package com.bluecone.app.infra.publicid.config;

import java.util.Collections;
import java.util.List;

/**
 * YAML 根节点配置。
 */
public class PublicIdResourcesConfig {
    private List<PublicIdResourceDefinition> resources;

    public List<PublicIdResourceDefinition> getResources() {
        return resources == null ? Collections.emptyList() : resources;
    }

    public void setResources(List<PublicIdResourceDefinition> resources) {
        this.resources = resources;
    }
}

