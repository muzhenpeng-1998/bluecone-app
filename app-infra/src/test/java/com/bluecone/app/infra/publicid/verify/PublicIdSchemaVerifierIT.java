package com.bluecone.app.infra.publicid.verify;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluecone.app.infra.publicid.config.PublicIdResourceDefinitionLoader;
import com.bluecone.app.infra.test.AbstractIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "bluecone.publicid.verify.enabled=false")
class PublicIdSchemaVerifierIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldFailFastWhenIndexMissing() throws Exception {
        jdbcTemplate().execute("DROP TABLE IF EXISTS bc_pid_missing_idx");
        jdbcTemplate().execute("""
                CREATE TABLE bc_pid_missing_idx (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    public_id VARCHAR(64) NOT NULL
                )
                """);
        writeYaml("""
                resources:
                  - type: STORE
                    table: bc_pid_missing_idx
                    pkColumn: id
                    tenantColumn: tenant_id
                    publicIdColumn: public_id
                """);

        PublicIdVerifyProperties props = new PublicIdVerifyProperties();
        props.setEnabled(true);
        props.setFailFast(true);

        PublicIdSchemaVerifier verifier = new PublicIdSchemaVerifier(props, new PublicIdResourceDefinitionLoader(), dataSource);
        assertThatThrownBy(verifier::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少联合索引");
    }

    @Test
    void shouldPassWhenCompositeIndexExists() throws Exception {
        jdbcTemplate().execute("DROP TABLE IF EXISTS bc_pid_with_idx");
        jdbcTemplate().execute("""
                CREATE TABLE bc_pid_with_idx (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    public_id VARCHAR(64) NOT NULL
                )
                """);
        jdbcTemplate().execute("CREATE INDEX idx_bc_pid_with_idx_tenant_public ON bc_pid_with_idx(tenant_id, public_id)");
        writeYaml("""
                resources:
                  - type: STORE
                    table: bc_pid_with_idx
                    pkColumn: id
                    tenantColumn: tenant_id
                    publicIdColumn: public_id
                """);

        PublicIdVerifyProperties props = new PublicIdVerifyProperties();
        props.setEnabled(true);
        props.setFailFast(true);

        PublicIdSchemaVerifier verifier = new PublicIdSchemaVerifier(props, new PublicIdResourceDefinitionLoader(), dataSource);
        verifier.afterSingletonsInstantiated(); // should not throw
    }

    private void writeYaml(String content) throws Exception {
        Path target = Path.of("target/test-classes/public-id-resources.yaml");
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}

