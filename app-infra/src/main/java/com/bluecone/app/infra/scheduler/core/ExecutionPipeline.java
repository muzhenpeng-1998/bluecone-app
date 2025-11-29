package com.bluecone.app.infra.scheduler.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 简单的执行管线，支持前后置 Hook。
 */
public class ExecutionPipeline {

    private final List<Consumer<JobContext>> preHooks = new ArrayList<>();
    private final List<Consumer<JobContext>> postHooks = new ArrayList<>();

    public void addPreHook(Consumer<JobContext> hook) {
        preHooks.add(Objects.requireNonNull(hook));
    }

    public void addPostHook(Consumer<JobContext> hook) {
        postHooks.add(Objects.requireNonNull(hook));
    }

    public void execute(JobContext context, Runnable action) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(action, "action must not be null");
        preHooks.forEach(h -> safeRun(h, context));
        try {
            action.run();
        } finally {
            postHooks.forEach(h -> safeRun(h, context));
        }
    }

    private void safeRun(Consumer<JobContext> hook, JobContext context) {
        try {
            hook.accept(context);
        } catch (Exception ignored) {
            // Hook 不影响主流程
        }
    }
}
