package io.github.xuanyangyang.scheduling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 抽象延迟任务
 *
 * @author xuanyangyang
 * @since 2020/3/26 18:15
 */
public abstract class AbstractDelayedTask implements DelayedTask {
    /**
     * 任务名
     */
    private final String name;
    /**
     * 任务执行体
     */
    private final Runnable action;
    /**
     * 是否异步执行
     */
    private boolean async;
    /**
     * 执行时间
     */
    private long executeTime;
    /**
     * 初始化
     */
    private boolean init;
    /**
     * 调度服务
     */
    private ScheduledService scheduledService;

    public AbstractDelayedTask(String name, Runnable action) {
        this(name, action, false);
    }

    protected AbstractDelayedTask(String name, Runnable action, boolean async) {
        this.name = name;
        this.action = action;
        this.async = async;
    }

    @Override
    public long executeTime() {
        return executeTime;
    }

    @Override
    public void run() {
        action.run();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean async() {
        return async;
    }

    public void init() {
        if (init) {
            return;
        }
        executeTime = firstExecuteTime();
    }

    @Override
    public CompletableFuture<Boolean> cancel() {
        if (scheduledService == null) {
            return CompletableFuture.completedFuture(false);
        }
        return scheduledService.removeTask(this);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(executeTime - System.currentTimeMillis(), 0), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }

    /**
     * @return 获取第一次执行时间，单位毫秒
     */
    protected abstract long firstExecuteTime();

    /**
     * 设置执行时间
     *
     * @param executeTime 执行时间，单位毫秒
     */
    protected void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public void setScheduledService(ScheduledService scheduledService) {
        this.scheduledService = scheduledService;
    }
}
