package com.bluecone.app.core.log;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bluecone.log")
public class LogProperties {

    private boolean enabled = true;
    private List<String> maskFields = Arrays.asList(
            "password", "pwd", "token", "secret", "accessToken",
            "refreshToken", "idCard", "phone", "mobile", "bankCard"
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getMaskFields() {
        return maskFields;
    }

    public void setMaskFields(List<String> maskFields) {
        this.maskFields = maskFields;
    }
}
