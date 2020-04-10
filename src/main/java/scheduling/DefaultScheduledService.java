package scheduling;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 调度服务
 *
 * @author xuanyangyang
 * @since 2020/3/26 18:07
 */
public class DefaultScheduledService implements ScheduledService {
    /**
     * 任务列表
     */
    private final DelayQueue<DelayedTask> tasks = new DelayQueue<>();
    /**
     * 运行中
     */
    private volatile boolean running = true;
    /**
     * 上次检查时间
     */
    private long lastCheckTime;
    /**
     * 添加任务锁，用于防止添加任务和刷新任务的时候因为并发而丢失任务
     */
    private final Object addTaskLock = new Object();
    /**
     * 等待添加的异步周期任务，用于辅助删除异步周期任务
     */
    private final Set<DelayedTask> waitAddAsyncTasks = new HashSet<>();
    /**
     * 任务执行器
     */
    private final Executor executor;
    /**
     * 异步任务执行器
     */
    private final Executor asyncExecutor;

    public DefaultScheduledService(Executor asyncExecutor) {
        this.executor = Executors.newSingleThreadExecutor(new CustomizableThreadFactory("调度线程"));
        this.asyncExecutor = asyncExecutor;
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
        synchronized (addTaskLock) {
            tasks.add(task);
        }

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
                e.printStackTrace();
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

    /**
     * 初始化
     */
    public void init() {
        executor.execute(this::runTasks);
        lastCheckTime = System.currentTimeMillis();
        addTask("刷新定时任务", 3, 3, TimeUnit.SECONDS, this::tryRefreshTask);
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
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    /**
     * 刷新定时任务
     */
    private void tryRefreshTask() {
        long now = System.currentTimeMillis();
        if (lastCheckTime + 10000 >= now) {
            lastCheckTime = now;
            return;
        }
        lastCheckTime = now;
        synchronized (addTaskLock) {
            List<DelayedTask> tempTasks = new ArrayList<>(tasks);
            tasks.clear();
            tasks.addAll(tempTasks);
        }
    }
}
