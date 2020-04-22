package io.github.xuanyangyang.scheduling;

import java.util.concurrent.Delayed;

/**
 * 延迟任务
 *
 * @author xuanyangyang
 * @since 2020/3/26 18:07
 */
public interface DelayedTask extends Delayed, ScheduledFuture {
    /**
     * @return 执行时间
     */
    long executeTime();

    /**
     * 运行任务
     */
    void run();

    /**
     * @return 任务名
     */
    String name();

    /**
     * @return 异步任务
     */
    boolean async();
}
