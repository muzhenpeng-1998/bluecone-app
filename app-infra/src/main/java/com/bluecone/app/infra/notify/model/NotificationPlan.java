package com.bluecone.app.infra.notify.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Policy 层产物：一个 Intent 经过路由得到多个通道任务。
 */
public class NotificationPlan {

    private final NotificationIntent intent;
    private final List<NotificationTask> tasks;

    public NotificationPlan(NotificationIntent intent, List<NotificationTask> tasks) {
        this.intent = Objects.requireNonNull(intent, "intent must not be null");
        this.tasks = tasks == null ? List.of() : new ArrayList<>(tasks);
    }

    public NotificationIntent getIntent() {
        return intent;
    }

    public List<NotificationTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }
}
