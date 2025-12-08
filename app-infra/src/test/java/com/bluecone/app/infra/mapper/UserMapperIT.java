package com.bluecone.app.infra.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.core.user.infra.persistence.entity.UserEntity;
import com.bluecone.app.core.user.infra.persistence.mapper.UserMapper;
import com.bluecone.app.infra.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserMapperIT extends AbstractIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    void insertsAndReadsUser() {
        UserEntity entity = new UserEntity();
        entity.setTenantId(888L);
        entity.setUsername("integration-user");
        entity.setPasswordHash("secret");
        entity.setStatus("ACTIVE");

        userMapper.insert(entity);

        UserEntity loaded = userMapper.selectById(entity.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getUsername()).isEqualTo("integration-user");
        assertThat(loaded.getTenantId()).isEqualTo(888L);
    }
}
