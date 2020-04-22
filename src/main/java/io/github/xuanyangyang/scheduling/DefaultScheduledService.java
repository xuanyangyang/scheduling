package io.github.xuanyangyang.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 调度服务
 *
 * @author xuanyangyang
 * @since 2020/3/26 18:07
 */
public class DefaultScheduledService implements ScheduledService, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 任务列表
     */
    private final RefreshDelayQueue<DelayedTask> tasks;
    /**
     * 运行中
     */
    private volatile boolean running = true;
    /**
     * 上次检查时间
     */
    private long lastCheckTime;
    /**
     * 等待添加的异步周期任务，用于辅助删除异步周期任务
     */
    private final Set<DelayedTask> waitAddAsyncTasks = ConcurrentHashMap.newKeySet();
    /**
     * 任务执行器
     */
    private final Executor executor;
    /**
     * 异步任务执行器
     */
    private final Executor asyncExecutor;
    /**
     * 是否启用自动刷新机制
     */
    private final boolean enableAutoRefresh;
    /**
     * 自动刷新初始延迟（毫秒）
     */
    private final long autoRefreshDelay;
    /**
     * 自动刷新周期（毫秒）
     */
    private long autoRefreshPeriod;
    /**
     * 自动刷新阈值（毫秒）
     */
    private long autoRefreshThreshold;
    /**
     * 初始化
     */
    private final AtomicBoolean init = new AtomicBoolean();

    public DefaultScheduledService() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new CustomizableThreadFactory("异步调度线程-")));
    }

    public DefaultScheduledService(Executor asyncExecutor) {
        this("调度线程-", asyncExecutor, true,
                0, 3000, 10000);
    }

    public DefaultScheduledService(String threadNamePrefix, Executor asyncExecutor, boolean enableAutoRefresh,
                                   long autoRefreshDelay, long autoRefreshPeriod, long autoRefreshThreshold) {
        this.executor = Executors.newSingleThreadExecutor(new CustomizableThreadFactory(threadNamePrefix));
        this.asyncExecutor = asyncExecutor;
        this.tasks = RefreshDelayQueue.newQueue();
        this.enableAutoRefresh = enableAutoRefresh;
        this.autoRefreshDelay = autoRefreshDelay;
        this.autoRefreshPeriod = autoRefreshPeriod;
        this.autoRefreshThreshold = autoRefreshThreshold;
    }

    @Override
    public ScheduledFuture addTask(String name, String cron, Runnable action, boolean async) {
        return addTaskWithInit(new CronTask(name, action, async, cron));
    }

    @Override
    public ScheduledFuture addTask(String name, long delay, TimeUnit unit, Runnable action, boolean async) {
        return addTaskWithInit(new DefaultDelayedTask(name, action, async, delay, unit));
    }

    @Override
    public ScheduledFuture addTask(String name, long delay, long period, TimeUnit unit, Runnable action, boolean async) {
        return addTaskWithInit(new DefaultPeriodicTask(name, action, async, delay, period, unit));
    }

    /**
     * 添加任务且初始化
     *
     * @param task 任务
     * @return 任务future
     */
    public ScheduledFuture addTaskWithInit(AbstractDelayedTask task) {
        task.init();
        task.setScheduledService(this);
        return addTask(task);
    }

    @Override
    public ScheduledFuture addTask(DelayedTask task) {
        tasks.add(task);
        return task;
    }

    @Override
    public CompletableFuture<Boolean> removeTask(DelayedTask task) {
        return removeTask0(task.name(), () -> tasks.remove(task) | waitAddAsyncTasks.remove(task));
    }

    @Override
    public CompletableFuture<Boolean> removeTask(String taskName) {
        return removeTask0(taskName, () -> {
            Predicate<DelayedTask> filter = task -> task.name().equals(taskName);
            return tasks.removeIf(filter) | waitAddAsyncTasks.removeIf(filter);
        });
    }


    /**
     * 删除任务
     *
     * @param removeTaskSupplier 删除任务实际执行体
     * @return 删除结果
     */
    private CompletableFuture<Boolean> removeTask0(String taskName, Supplier<Boolean> removeTaskSupplier) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        execute("删除" + taskName + "定时任务", () -> {
            Boolean res;
            try {
                res = removeTaskSupplier.get();
            } catch (Exception e) {
                res = false;
                logger.error("删除定时任务失败", e);
            }
            future.complete(res);
        });
        return future;
    }

    /**
     * 立刻执行任务
     *
     * @param taskName 任务名
     * @param action   任务体
     * @param async    异步
     * @return 任务future
     */
    public ScheduledFuture execute(String taskName, Runnable action, boolean async) {
        return addTask(taskName, 0, TimeUnit.MILLISECONDS, action, async);
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void start() {
        init();
    }

    /**
     * 初始化
     */
    public void init() {
        if (init.compareAndSet(false, true)) {
            executor.execute(this::runTasks);
            lastCheckTime = System.currentTimeMillis();
            if (enableAutoRefresh) {
                addTask("刷新定时任务", autoRefreshDelay, autoRefreshPeriod, TimeUnit.MILLISECONDS, this::tryRefreshTask);
            }
        }
    }

    /**
     * 运行任务
     */
    private void runTasks() {
        while (running) {
            try {
                DelayedTask task = tasks.take();
                if (task.async()) {
                    runAsyncTask(task);
                } else {
                    runTask(task);
                }
            } catch (Exception e) {
                logger.error("运行定时任务错误", e);
            }
        }
    }

    /**
     * 运行异步任务
     *
     * @param task 任务
     */
    private void runAsyncTask(DelayedTask task) {
        if (task instanceof PeriodicTask) {
            waitAddAsyncTasks.add(task);
        }
        asyncExecutor.execute(() -> {
            safeRunTask(task);
            if (task instanceof PeriodicTask) {
                execute("添加" + task.name() + "任务", () -> {
                    if (waitAddAsyncTasks.remove(task)) {
                        addTask(task);
                    }
                });
            }
        });
    }

    /**
     * 运行任务
     *
     * @param task 任务
     */
    private void runTask(DelayedTask task) {
        safeRunTask(task);
        if (task instanceof PeriodicTask) {
            addTask(task);
        }
    }

    /**
     * 安全运行任务
     *
     * @param task 任务
     */
    private void safeRunTask(DelayedTask task) {
        try {
            task.run();
        } catch (Exception e) {
            logger.error("运行定时任务错误", e);
        }
    }

    /**
     * 刷新定时任务
     */
    private void tryRefreshTask() {
        long now = System.currentTimeMillis();
        if (lastCheckTime + autoRefreshThreshold >= now) {
            lastCheckTime = now;
            return;
        }
        lastCheckTime = now;
        tasks.refresh();
    }

    @Override
    public void destroy() {
        stop();
    }

    public boolean isEnableAutoRefresh() {
        return enableAutoRefresh;
    }

    public long getAutoRefreshDelay() {
        return autoRefreshDelay;
    }

    public long getAutoRefreshPeriod() {
        return autoRefreshPeriod;
    }

    public long getAutoRefreshThreshold() {
        return autoRefreshThreshold;
    }

    public void setAutoRefreshPeriod(long autoRefreshPeriod) {
        this.autoRefreshPeriod = autoRefreshPeriod;
    }

    public void setAutoRefreshThreshold(long autoRefreshThreshold) {
        this.autoRefreshThreshold = autoRefreshThreshold;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isInit() {
        return init.get();
    }
}
