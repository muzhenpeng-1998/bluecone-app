package com.bluecone.app.infra.notify.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 场景级配置（Policy 层）。
 */
public class NotifyScenarioConfig {

    private String scenarioCode;
    private boolean enabled = true;
    private List<NotifyChannelConfig> channels = new ArrayList<>();

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<NotifyChannelConfig> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    public void setChannels(List<NotifyChannelConfig> channels) {
        this.channels = channels == null ? new ArrayList<>() : new ArrayList<>(channels);
    }
}
