package com.bluecone.app.platform.codegen.publicid;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicIdLookupCodegenMainTest {

    @Test
    void shouldGenerateLookupClassesFromYaml() throws Exception {
        Path tempDir = Files.createTempDirectory("publicid-codegen-test");
        Path yaml = tempDir.resolve("public-id-resources.yaml");
        Files.writeString(yaml, """
                resources:
                  - type: STORE
                    table: bc_store
                    pkColumn: id
                    tenantColumn: tenant_id
                    publicIdColumn: public_id
                  - type: PRODUCT
                    table: bc_product
                    pkColumn: id
                    tenantColumn: tenant_id
                    publicIdColumn: public_id
                """);

        Path outputDir = tempDir.resolve("generated");
        PublicIdLookupCodegenMain.main(new String[]{yaml.toString(), outputDir.toString(), "128"});

        Path storeFile = outputDir.resolve("com/bluecone/app/infra/publicid/lookup/StorePublicIdLookup.java");
        Path productFile = outputDir.resolve("com/bluecone/app/infra/publicid/lookup/ProductPublicIdLookup.java");
        Path configFile = outputDir.resolve("com/bluecone/app/infra/publicid/lookup/PublicIdLookupGeneratedConfiguration.java");

        assertThat(Files.exists(storeFile)).isTrue();
        assertThat(Files.exists(productFile)).isTrue();
        assertThat(Files.exists(configFile)).isTrue();

        String storeCode = Files.readString(storeFile);
        assertThat(storeCode).contains("ResourceType.STORE");
        assertThat(storeCode).contains("SELECT id FROM bc_store");
        assertThat(storeCode).contains("public_id IN (:publicIds)");
        assertThat(storeCode).contains("MAX_BATCH_SIZE = 128");

        String configCode = Files.readString(configFile);
        assertThat(configCode).contains("storePublicIdLookup");
        assertThat(configCode).contains("productPublicIdLookup");
    }
}

