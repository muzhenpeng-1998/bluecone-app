package com.bluecone.app.infra.notify.delivery;

import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationTask;

import java.util.Objects;

/**
 * Delivery 层的信封，打包 Intent 与单通道任务。
 */
public class NotificationEnvelope {

    private final NotificationIntent intent;
    private final NotificationTask task;

    public NotificationEnvelope(NotificationIntent intent, NotificationTask task) {
        this.intent = Objects.requireNonNull(intent, "intent must not be null");
        this.task = Objects.requireNonNull(task, "task must not be null");
    }

    public NotificationIntent getIntent() {
        return intent;
    }

    public NotificationTask getTask() {
        return task;
    }
}
