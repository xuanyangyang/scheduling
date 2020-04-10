package scheduling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 调度管理
 *
 * @author xuanyangyang
 * @since 2020/3/27 17:03
 */
public interface ScheduledService {

    /**
     * 添加任务
     *
     * @param name   任务名
     * @param cron   cron 表达式
     * @param action 任务体
     * @return 任务future
     */
    default ScheduledFuture addTask(String name, String cron, Runnable action) {
        return addTask(name, cron, action, false);
    }

    /**
     * 添加任务
     *
     * @param name   任务名
     * @param cron   cron 表达式
     * @param action 任务体
     * @param async  异步
     * @return 任务future
     */
    ScheduledFuture addTask(String name, String cron, Runnable action, boolean async);

    /**
     * 添加任务
     *
     * @param name   任务名
     * @param delay  延迟多久
     * @param unit   时间单位
     * @param action 任务体
     * @return 任务future
     */
    default ScheduledFuture addTask(String name, long delay, TimeUnit unit, Runnable action) {
        return addTask(name, delay, unit, action, false);
    }

    /**
     * 添加任务
     *
     * @param name   任务名
     * @param delay  延迟多久
     * @param unit   时间单位
     * @param action 任务体
     * @param async  异步
     * @return 任务future
     */
    ScheduledFuture addTask(String name, long delay, TimeUnit unit, Runnable action, boolean async);

    /**
     * 添加任务
     *
     * @param name   任务名
     * @param delay  延迟多久
     * @param period 周期
     * @param unit   时间单位
     * @param action 任务体
     * @return 任务future
     */
    default ScheduledFuture addTask(String name, long delay, long period, TimeUnit unit, Runnable action) {
        return addTask(name, delay, period, unit, action, false);
    }

    /**
     * 添加任务
     *
     * @param name   任务名
     * @param delay  延迟多久
     * @param period 周期
     * @param unit   时间单位
     * @param action 任务体
     * @param async  异步
     * @return 任务future
     */
    ScheduledFuture addTask(String name, long delay, long period, TimeUnit unit, Runnable action, boolean async);

    /**
     * 添加任务
     *
     * @param task 任务
     * @return 任务future
     */
    ScheduledFuture addTask(DelayedTask task);

    /**
     * 通过任务实例删除任务
     *
     * @param task 任务
     * @return 删除结果
     */
    CompletableFuture<Boolean> removeTask(DelayedTask task);

    /**
     * 通过任务名删除任务
     *
     * @param taskName 任务名
     * @return 删除结果
     */
    CompletableFuture<Boolean> removeTask(String taskName);

    /**
     * 立刻执行任务
     *
     * @param taskName 任务名
     * @param action   任务体
     * @return 任务future
     */
    default ScheduledFuture execute(String taskName, Runnable action) {
        return execute(taskName, action, false);
    }

    /**
     * 立刻执行任务
     *
     * @param taskName 任务名
     * @param action   任务体
     * @param async    异步
     * @return 任务future
     */
    ScheduledFuture execute(String taskName, Runnable action, boolean async);
}
